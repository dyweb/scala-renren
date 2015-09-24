package renren

// project utils
import utils.Reporters._
import utils.HttpParser
import utils.Dumper

// httpclient
import java.io._
import org.apache.commons._
import org.apache.http._
import org.apache.http.client._
import org.apache.http.client.methods.{ HttpPost, HttpGet }
import java.util.{ ArrayList }
import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.impl.client.{ BasicResponseHandler, DefaultHttpClient }
import org.apache.http.client.params.{ CookiePolicy, HttpClientParams }
import org.apache.http.util.EntityUtils
import org.apache.http.params.CoreProtocolPNames

class RenrenHttpClient(reporter: Reporter, username: String, pwd: String) {
  val client = new DefaultHttpClient
  client.getParams().setParameter("http.protocol.single-cookie-header", true)
  HttpClientParams.setCookiePolicy(client.getParams(), CookiePolicy.BROWSER_COMPATIBILITY)

  def neededOperation(new_client: DefaultHttpClient) = {
    val url = "http://www.renren.com/ajaxLogin/login"

    // login, first
    var httpPost = new HttpPost(url)
    var nvps = new ArrayList[NameValuePair]()
    nvps.add(new BasicNameValuePair("domain", "renren.com"))
    nvps.add(new BasicNameValuePair("isplogin", "true"))
    nvps.add(new BasicNameValuePair("formName", ""))
    nvps.add(new BasicNameValuePair("method", ""))
    nvps.add(new BasicNameValuePair("submit", "登录"))
    nvps.add(new BasicNameValuePair("email", username))
    nvps.add(new BasicNameValuePair("password", pwd))
    httpPost.setEntity(new UrlEncodedFormEntity(nvps))
    var res = new_client.execute(httpPost)
    var entity = res.getEntity()
    EntityUtils.consume(entity)
  }

  neededOperation(client)

  def getContentByUrl(url: String): String = {
    val useragents = List(
      "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.57 Safari/537.17",
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Ubuntu Chromium/23.0.1271.97 Chrome/23.0.1271.97 Safari/537.11",
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.56 Safari/537.17",
      "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.34 (KHTML, like Gecko) rekonq/1.1 Safari/534.34",
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/536.26.17 (KHTML, like Gecko) Version/6.0.2 Safari/536.26.17")
    client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, useragents(scala.util.Random.nextInt(5)));

    val httpGet = new HttpGet(url)
    val res_new = client.execute(httpGet)
    var rd = new BufferedReader(new InputStreamReader(res_new.getEntity().getContent()))
    var content = ""
    var line = rd.readLine()
    while (line != null) {
      content += line
      line = rd.readLine()
    }

    // close the connection
    val entity = res_new.getEntity()
    EntityUtils.consume(entity)

    // return 
    content
  }
}