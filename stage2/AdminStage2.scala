package cbt
object AdminStage2{
  def main(args: Array[String]) = {
    val init = new Init(args.drop(3))
    val lib = new Lib(init.logger)
    val adminTasks = new AdminTasks(lib, args.drop(3))
    new lib.ReflectObject(adminTasks){
      def usage: String = "Available methods: " ++ lib.taskNames(subclassType).mkString("  ")
    }.callNullary(args.lift(2))
  }
}
