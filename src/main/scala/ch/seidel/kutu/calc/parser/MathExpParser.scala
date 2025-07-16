package ch.seidel.kutu.calc.parser

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{NoPosition, Position, Reader}

protected object MathExpParser extends Parsers {
  override type Elem = MathExpression

  private class MathExpressionReader(tokens: Seq[MathExpression]) extends Reader[MathExpression] {
    override def first: MathExpression = tokens.head
    override def atEnd: Boolean = tokens.isEmpty
    override def pos: Position = NoPosition
    override def rest: Reader[MathExpression] = new MathExpressionReader(tokens.tail)
  }

  private def constant: Parser[MathExpAST] = accept("constant", {
    case x: NUMBER => Constant(x.value)
  })

  def variable: Parser[Variable] = accept("value", {
    case VAR_NAME(x) => Variable(x)
  })

  private def functionName: Parser[MathExpAST] = accept("function name", {
    case f: FUNC_NAME => Constant(f.name)
  })

  def function: Parser[MathExpAST] =
    functionName ~ (
      LEFT_PARENTHESIS ~> expression ~ rep(COMMA ~> expression) <~ RIGHT_PARENTHESIS) ^^ {
      case Constant(n) ~ (e ~ es) => OperatorN(n.toString, e :: es)
    }

  private def shortFactor: Parser[MathExpAST] =
    constant | variable | function | LEFT_PARENTHESIS ~> expression <~ RIGHT_PARENTHESIS ^^
      { case x => x }

  private def longFactor: Parser[MathExpAST] = shortFactor ~ rep(POWER ~ shortFactor) ^^ {
    case x ~ ls => ls.foldLeft[MathExpAST](x) {
      case (d1, POWER ~ d2) => Operator2(POWER.toString, d1, d2)
    }
  }

  def term: Parser[MathExpAST] = longFactor ~ rep((MULTIPLY | DIVIDE) ~ longFactor) ^^ {
    case x ~ ls => ls.foldLeft[MathExpAST](x) {
      case (d1, MULTIPLY ~ d2) => Operator2(MULTIPLY.toString, d1, d2)
      case (d1, DIVIDE ~ d2) => Operator2(DIVIDE.toString, d1, d2)
    }
  }

  def expression: Parser[MathExpAST] = term ~ rep((PLUS | MINUS) ~ term) ^^ {
    case x ~ ls => ls.foldLeft[MathExpAST](x) {
      case (d1, PLUS ~ d2) => Operator2(PLUS.toString, d1, d2)
      case (d1, MINUS ~ d2) => Operator2(MINUS.toString, d1, d2)
    }
  }

  private def program: Parser[MathExpAST] = phrase(expression)

  def apply(tokens: Seq[MathExpression]): MathExpAST = {
    val reader = new MathExpressionReader(tokens)

    program(reader) match {
      case Failure(msg, next) => throw new IllegalArgumentException(msg)
      case Error(msg, next) => throw new IllegalArgumentException(msg)
      case NoSuccess(msg, next) => throw new IllegalArgumentException(msg)
      case Success(result, next) => result
    }
  }
}


