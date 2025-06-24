package ch.seidel.kutu.calc.parser

sealed trait MathExpression

sealed trait Operation extends MathExpression
case object PLUS extends Operation {
  override def toString: String = "+"
}
case object MINUS extends Operation {
  override def toString: String = "-"
}
case object MULTIPLY extends Operation {
  override def toString: String = "*"
}
case object DIVIDE extends Operation {
  override def toString: String = "/"
}
case object POWER extends Operation {
  override def toString: String = "**"
}

sealed trait Delimiter extends Operation
case object COMMA extends Delimiter {
  override def toString: String = ","
}
case object LEFT_PARENTHESIS extends Delimiter {
  override def toString: String = "("
}
case object RIGHT_PARENTHESIS extends Delimiter {
  override def toString: String = ")"
}

sealed trait Value extends MathExpression
case class NUMBER(value: Double) extends Value {
  override def toString: String = value.toString
}

case class VAR_NAME(name: String) extends MathExpression

case class FUNC_NAME(name: String) extends MathExpression
