package ch.seidel.kutu.calc.parser

object MathExpCompiler {
  def apply(code: String): MathExpAST = {
    val mep = MathExpParser(MathExpScanner(code))
    println("mpe:", mep)
    mep
  }
}