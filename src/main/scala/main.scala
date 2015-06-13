import config.Parser
import renren.Renren
import utils.Reporters._
import utils.Dumper

object Main {
	val reporter: Reporter = new Reporter()
	reporter.attach(new ConsoleReporterHandler())
    reporter.open()
    val dumper: Dumper = new Dumper("Output-Data.txt")

	def main(args: Array[String]): Unit = {
		val parser = new Parser("userinfo.ini", reporter)
		parser.run()
		val username = parser.getUsername()
		val pwd = parser.getPwd()

		val renren = new Renren(username, pwd, reporter, dumper)
		renren.runSixDegreeVerifier

	}
}