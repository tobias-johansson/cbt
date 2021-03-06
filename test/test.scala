import cbt._
import cbt.paths._
import scala.collection.immutable.Seq

// micro framework
object Main{
  def main(args: Array[String]): Unit = {
    val init = new Init(args)
    implicit val logger: Logger = init.logger
    
    var successes = 0
    var failures = 0
    def assert(condition: Boolean, msg: String = "")(implicit logger: Logger) = {
      scala.util.Try{
        Predef.assert(condition, "["++msg++"]")
      }.map{ _ =>
        print(".")
        successes += 1
      }.recover{
        case e: AssertionError =>
          println("FAILED")
          e.printStackTrace
          failures += 1
      }.get
    }

    def runCbt(path: String, args: Seq[String])(implicit logger: Logger): Result = {
      import java.io._
      val allArgs: Seq[String] = ((cbtHome.string ++ "/cbt") +: "direct" +: (args ++ init.propsRaw))
      logger.test(allArgs.toString)
      val pb = new ProcessBuilder( allArgs :_* )
      pb.directory(cbtHome ++ ("/test/" ++ path))
      val p = pb.inheritIO.start
      p.waitFor
      val berr = new BufferedReader(new InputStreamReader(p.getErrorStream));
      val bout = new BufferedReader(new InputStreamReader(p.getInputStream));
      p.waitFor
      import collection.JavaConversions._
      val err = Stream.continually(berr.readLine()).takeWhile(_ != null).mkString("\n")
      val out = Stream.continually(bout.readLine()).takeWhile(_ != null).mkString("\n")
      Result(out, err, p.exitValue == 0)
    }
    case class Result(out: String, err: String, exit0: Boolean)
    def assertSuccess(res: Result)(implicit logger: Logger) = {
      assert(res.exit0, res.toString)
    }

    // tests
    def usage(path: String)(implicit logger: Logger) = {
      val usageString = "Methods provided by CBT"
      val res = runCbt(path, Seq())
      logger.test(res.toString)
      assertSuccess(res)
      assert(res.out == "", res.toString)
      assert(res.err contains usageString, res.toString)
    }
    def compile(path: String)(implicit logger: Logger) = {
      val res = runCbt(path, Seq("compile"))
      assertSuccess(res)
      // assert(res.err == "", res.err) // FIXME: enable this
    }

    logger.test( "Running tests " ++ args.toList.toString )

    usage("nothing")
    compile("nothing")

    {
      val noContext = Context(cbtHome ++ "/test/nothing", Seq(), logger)
      val b = new Build(noContext){
        override def dependencies = Seq(
          MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0")(logger),
          MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0")(logger)
        )
      }
      val cp = b.classpath
      assert(cp.strings.distinct == cp.strings, "duplicates in classpath: " ++ cp.string)
    }

    System.err.println(" DONE!")
    System.err.println( successes.toString ++ " succeeded, "++ failures.toString ++ " failed" )
    if(failures > 0) System.exit(1) else System.exit(0)
  }
}
