package ch.seidel.kutu.calc.parser

sealed trait MathExpAST

case class Constant[+A](get: A) extends MathExpAST {
  override def toString: String = get.toString
}

case class Variable(name: String) extends MathExpAST

case class Operator2(op: String, v1: MathExpAST, v2: MathExpAST) extends MathExpAST {
  override def toString: String = s"($op, $v1, $v2)"
}
case class OperatorN(op: String, vs: List[MathExpAST]) extends MathExpAST
