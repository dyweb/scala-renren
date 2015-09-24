package renren

import scala.collection.mutable.{ HashMap }
import scala.collection.mutable.Queue
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
import java.nio.file.{ Paths, Files }

class SixDegreeVerifier(reporter: Reporter, renren: Renren) extends NetworkBase(reporter: Reporter) {
  // initialize()
  var visitedSet: Set[Friend] = Set()
  var queue: Queue[Friend] = new Queue()
  val shore: Friend = new Friend("-1", "-1", "-1", "-1")
  var dis = 0
  var fuid: String = ""
  class WorkAcotr() extends Actor {
    def act() {
      val new_renren = new Renren(renren.username, renren.pwd, renren.reporter, renren.dumper)
      while (true) {
        receive {
          case "recruit" =>
            reporter.info("Ready to work")
            sender ! "end"
          case k: Friend =>
            if (visitedSet contains k) {
              sender ! "end"
            } else {
              if (k == shore) {
                dis += 1
                queue.enqueue(shore)
                sender ! "end"
              } else {
                visitedSet += k
                if (k.uid == fuid) {
                  reporter.msg("Has Found the frind in depth " + dis)
                  sender ! "Found"
                } else {
                  queue ++= translate2Friend(new_renren.getFriendList(k.uid))
                  sender ! "end"
                }
              }
            }
        }
      }
    }
  }

  class SuperActor(thread: Int) extends Actor {
    var actorList: List[WorkAcotr] = List()
    for (i <- 1 to thread) {
      reporter.info("create " + i.toString + "th actor")
      val actor = new WorkAcotr()
      actorList = actor :: actorList
    }
    def act() = {
      while (true) {
        receive {
          case "begin" =>
            reporter.info("superActor begins to work")
            for (actor <- actorList) {
              actor.start ! "recruit"
            }
          case "end" =>
            if (!queue.isEmpty) {
              val f = queue.dequeue
              reporter.info("Begin to analyze " + f.toString)
              sender ! f
            } else {
              reporter.info("Not found")
              exit()
            }
          case "Found" =>
            exit()
        }
      }
    }
  }

  def findShortestPath(uid: String, fuid: String) = {
    this.fuid = fuid
    val new_renren = new Renren(renren.username, renren.pwd, renren.reporter, renren.dumper)
    queue ++= translate2Friend(new_renren.getFriendList(uid))
    queue.enqueue(shore)
    var sa = new SuperActor(4)
    sa.start ! "begin"
  }
}