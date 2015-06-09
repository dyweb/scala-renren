package utils

// jsoup
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// project utils
import utils.Reporters._

// utilities
import scala.collection.{ mutable, immutable, generic }

class HttpParser(username: String, reporter: Reporter, dumper: Dumper) {
	def getUid(content: String): String = {
		var document = Jsoup.parse(content)
		var linkList = document.select("a[href~=renren.com/[0-9]+/profile]")
		
		// can't use for or map to iterate the Elements
		var href: java.util.Iterator[Element] = linkList.iterator()
		var objectName = href.next()
		var hrefValue = objectName.attr("href");
		val uid = hrefValue.split("/")(3)
		reporter.info(username + "'s uid is " + uid)
		uid
	}

	def getFriendCount(content: String): Int = {
		var document = Jsoup.parse(content)
		var linkList = document.select("span[class=count]")
		
		// can't use for or map to iterate the Elements
		val href: java.util.Iterator[Element] = linkList.iterator()
		var objectName = href.next()
		var count = objectName.html
		reporter.info(" has " + count.toString + " friends")
		count.toInt
	}

	def getFriendsInOnePage(content: String): List[Element] = {
		var document = Jsoup.parse(content)
		var linkList = document.select("ol[id=friendListCon]")
		if (linkList.size != 0) {
			var elements = linkList.iterator().next().children()
			var list: List[Element] = List()
			var it: java.util.Iterator[Element] = elements.iterator()
			while (it.hasNext()) {
				var obj = it.next()
				list = obj :: list
			}
			list
		}
		else {
			Nil
		}
	}

	def getSchoolRank(friends: List[Element]) = {
		var hmp = new scala.collection.mutable.ListMap[String, Int]()
		for( friend <- friends) {
			var infos = friend.select("dt")
			var it: java.util.Iterator[Element] = infos.iterator()

			// remove name
			it.next()
			
			var schoolInfo = it.next().html()
			if (schoolInfo.toString == "学校") {
				infos = friend.select("dd")
				it = infos.iterator()

				it.next()

				schoolInfo = it.next().html()
				hmp(schoolInfo) = (hmp getOrElse (schoolInfo, 0)) + 1
			}
		}

		val list = hmp.toList.sortWith(_._2 > _._2)
		dumper.writeSchoolRank(list)
	}
}