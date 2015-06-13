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

class NetworkBase(reporter: Reporter) {
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
}