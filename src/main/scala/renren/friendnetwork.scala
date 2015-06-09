package renren

import scala.collection.mutable.{ HashMap }
import scala.collection.mutable.ListBuffer
import scala.Serializable
import scala.io.Source
import scala.actors.Actor
import scala.actors.Actor._

// jsoup
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// project utils
import utils.Reporters._
import utils.Dumper
import utils.Grapher

import java.io._
import java.nio.file.{Paths, Files}

//jackson
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.pickling._
import scala.pickling.json._

class FriendNetwork(reporter: Reporter) {
	var filename = "network.txt"
	var hasFile = false
	if (Files.exists(Paths.get(filename))) {
		reporter.warn("found the network data in local disk, if you don't want it, rm the <network.txt>")
		hasFile = true
	}

	def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]

	// marshal class: responsible for dump a object to file and read it from file
	class Marshal(filename: String) {
		var obj: HashMap[Friend, List[Friend]] = new HashMap()
		var ctx: ListBuffer[String] = new ListBuffer()
		def run(): HashMap[Friend, List[Friend]] = {
			while (ctx.size != 0) {
				parseString2Network()
			}
			return obj
		}

		def parseString2Network() = {
			val line = ctx.head
			var friend: Friend = new Friend("", "", "", "")
			var list: List[Friend] = List()
			friend.parse(line.substring(1))
			ctx = ctx.tail
			if (ctx.size != 0) {
				list = parseString2List(friend, list)
			}
			obj(friend) = list
		}

		def parseString2List(friend: Friend, list: List[Friend]): List[Friend] = {
			val line = ctx.head
			if (line.charAt(0) == '-' && line.charAt(1) == '-') {
				var friendElement: Friend = new Friend("", "", "", "")
				friendElement.parse(line.substring(2))
				ctx = ctx.tail
				if (ctx.size != 0) {
					parseString2List(friend, friendElement :: list)
				}
				else {
					friendElement :: list
				}
			}
			else {
				list
			}
		}

		def dump(message: HashMap[Friend, List[Friend]] = HashMap()) = {
			reporter.msg("dump to <" + filename + ">")
			val dumper = new Dumper(filename)
			for( (k, v) <- message) {
				dumper.write("-")
				dumper.write(k.toString)
				dumper.write("\n")
				for( ele <- v) {
					dumper.write("--")
					dumper.write(ele.toString)
					dumper.write("\n")
				}
			}
			dumper.close
		}

		def read(): HashMap[Friend, List[Friend]] = {
			reporter.msg("read from <" + filename + ">")
			ctx.clear
			for(line <- Source.fromFile(filename)("UTF-8").getLines()) {
				ctx += line.toString()
			}

			run()
		}
	}

	var marshaler: Marshal = new Marshal("network.txt")
	var network: HashMap[Friend, List[Friend]] = new HashMap[Friend, List[Friend]]()
	def dump() = {
		marshaler.dump(network)
	}
	def read() = {
		network = marshaler.read()
	}

	// initial the network
	def initialize(list: List[Element]): Unit = {
		if (hasFile == true) {
			network = marshaler.read()
			return
		}
		for( ele <- list) {
			var infos = ele.select("div[class=info]")
			var it: java.util.Iterator[Element] = infos.iterator()
			var info = it.next()
			var value = info.select("dd").iterator()
			value.next()
			val _name: String = info.select("a").iterator().next().html()
			val _school: String = value.next().html()
			val _link: String = info.select("a").iterator().next().attr("href")
			val _uid: String = _link.split("=")(1)
			var friend = new Friend(_uid, _school, _name, _link)

			network getOrElseUpdate (friend, Nil)
		}
	}

	// transfer the List[Element] to List[Friend]
	def translate2Friend(list: List[Element]): List[Friend] = {
		var res: List[Friend] = List()
		for( ele <- list) {
			var infos = ele.select("div[class=info]")
			var it: java.util.Iterator[Element] = infos.iterator()
			var info = it.next()
			var value = info.select("dd").iterator()
			value.next()
			val _name: String = info.select("a").iterator().next().html()
			val _school: String = value.next().html()
			val _link: String = info.select("a").iterator().next().attr("href")
			val _uid: String = _link.split("=")(1)
			var friend = new Friend(_uid, _school, _name, _link)

			res = friend :: res
		}
		res
	}

	val me = this
	class FriendActor(renren: Renren, superActor: SuperActor) extends Actor {
		def act() = {
			var new_renren = new Renren(renren.username, renren.pwd, renren.reporter, renren.dumper)
			var flag = true
			while(flag) {
				receive {
					case k: Friend => 
					reporter.info("Begin parsing: " + k.toString)
					var workUid = k.uid
					// tricky
					if (new_renren.getFriendCount(workUid) < 1000) {
						var friList: List[Friend] = me.translate2Friend(new_renren.getFriendList(workUid))
						for( friend <- friList) {
							if (me.network.exists(_._1 == friend)) {
								me.network(k) = friend :: me.network(k)
							}
						}
					}
					sender ! "end"
					case "recruit" => 
					reporter.info("actor recruited")
					sender ! "end"
					case "Finish" => 
					sender ! "ready"
					reporter.info("actor exit")
					flag = false
					exit()
				}
			}
		}
	}

	class SuperActor(thread: Int, var list: List[(Friend, List[Friend])], renren: Renren) extends Actor {
		var actorList: List[FriendActor] = List()
		var flag = true
		var times = 50
		for( i <- 1 to thread) {
			reporter.info("create " + i.toString + "th actor")
			val actor = new FriendActor(renren, this)
			actorList = actor :: actorList
		}
		def act() = {
			var exitnum = thread
			while(flag) {
				receive {
					case "begin" => 
					reporter.info("superActor begins to work")
					for( actor <- actorList) {
						actor.start ! "recruit"
					}
					case "end" => 
					if (list.size != 0 && times != 0) {
						times = times - 1
						sender ! list.head._1
						list = list.tail
					}
					else {
						reporter.info("Finish The parsing")
						for( actor <- actorList) {
							actor ! "Finish"
						}
					}
					case "ready" => 
					exitnum = exitnum - 1
					reporter.msg(thread - exitnum + " Acotr(s) ready to exit")
					if (exitnum == 0) {
						flag = false
						reporter.info("Dumping to file")
						dump
						exportGraph
						exit
					}
				}
			}
		}
	}
	
	// parse all the friend you have and get the network
	def parseFriend(renren: Renren, thread: Int): Unit = {
		if (hasFile == true) {
			return
		}
		val me = this
		var list = network.toList

		val superActor: SuperActor = new SuperActor(thread, list, renren)
		superActor.start ! "begin"
		return
	}

	def exportGraph() = {
		val grapher = new Grapher(network)
		grapher.script()
	}
}