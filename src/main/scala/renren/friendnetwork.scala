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

class FriendNetwork(reporter: Reporter) extends NetworkBase(reporter: Reporter) {
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
					// flag = false
					exit()
				}
			}
		}
	}

	class SuperActor(thread: Int, var list: List[Friend], renren: Renren) extends Actor {
		var actorList: List[FriendActor] = List()
		var flag = true
		var times = -1
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
						sender ! list.head
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
			exportGraph
			return
		}
		val me = this
		var list = network.keySet.toList

		val superActor: SuperActor = new SuperActor(thread, list, renren)
		superActor.start ! "begin"
		// list.par.map {
		// 	ele => mapFunc(renren, ele)
		// }
		return
	}

	def exportGraph() = {
		val grapher = new Grapher(network, reporter)
		grapher.script()
	}
}