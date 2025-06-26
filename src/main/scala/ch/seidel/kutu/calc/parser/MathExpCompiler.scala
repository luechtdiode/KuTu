package ch.seidel.kutu.calc.parser

object MathExpCompiler {
  def apply(code: String): MathExpAST = {
    MathExpParser(MathExpScanner(code))
  }
}