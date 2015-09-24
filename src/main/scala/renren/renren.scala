package renren

// project utils
import utils.Reporters._
import utils.HttpParser
import utils.Dumper

import java.io._

// utilities
import scala.collection.{ mutable, immutable, generic }
import collection.JavaConversions._

// jsoup
import org.jsoup.Jsoup
import org.jsoup.helper.Validate
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class Renren(val username: String, val pwd: String, val reporter: Reporter, val dumper: Dumper) {

  // info 
  var uid = ""
  val httpParser: HttpParser = new HttpParser(username: String, reporter: Reporter, dumper: Dumper)
  var friends: List[Element] = Nil
  var network: FriendNetwork = new FriendNetwork(reporter)
  var renrenHttpClient: RenrenHttpClient = new RenrenHttpClient(reporter, username, pwd)

  private def divmod(x: Int, y: Int): (Int, Int) = {
    (x / y, x % y)
  }

  def login() = {
    val redirectURL = "http://www.renren.com/profile.do"

    // visit the profile page
    var content = renrenHttpClient.getContentByUrl(redirectURL)

    // parse profile for user
    uid = httpParser.getUid(content)

  }

  def getFriendList(_uid: String = ""): List[Element] = {
    var workUid: String = ""
    if (_uid == "") {
      workUid = uid
    } else {
      workUid = _uid
    }

    // get page num
    val elementsInOnePage = 20
    var page = 0
    var url = "http://friend.renren.com/GetFriendList.do?curpage=%s&id=" + workUid
    var content = renrenHttpClient.getContentByUrl(url format (page.toString()))
    var count = httpParser.getFriendCount(content)

    val internalRes = divmod(count, elementsInOnePage)
    val pageNum = internalRes._1 + (if (internalRes._2 == 0) 0 else 1)

    // get all friends
    var list: List[Element] = Nil
    for (pageBuf <- 0 to pageNum) {
      val pageUrl = url format pageBuf.toString
      val content = renrenHttpClient.getContentByUrl(pageUrl)
      list = list ::: httpParser.getFriendsInOnePage(content)
      reporter.info("getting the friend data in the page " + pageBuf)
    }
    list
  }

  def getFriendCount(_uid: String = ""): Int = {
    var workUid: String = ""
    if (_uid == "") {
      workUid = uid
    } else {
      workUid = _uid
    }

    var url = "http://friend.renren.com/GetFriendList.do?curpage=%s&id=" + workUid
    var content = renrenHttpClient.getContentByUrl(url format ("0"))
    var count = httpParser.getFriendCount(content)

    count
  }

  def getSchoolRank() = {
    if (friends.isEmpty) {
      reporter.error("you don't have a friend list now")
    }

    httpParser.getSchoolRank(friends)
  }

  def runFriendNetwork() = {
    login

    friends = getFriendList()
    getSchoolRank
    network.initialize(friends)
    // 4: The thread count
    network.parseFriend(this, 4)
    dumper.close
  }

  def runSixDegreeVerifier() = {
    login

    val sdv = new SixDegreeVerifier(reporter, this)
    sdv.findShortestPath(uid, "75511553")
  }
}