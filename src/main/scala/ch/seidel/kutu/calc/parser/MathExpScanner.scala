package ch.seidel.kutu.calc.parser

import scala.util.parsing.combinator.JavaTokenParsers

object MathExpScanner extends JavaTokenParsers {
  def plus: Parser[Operation] = PLUS.toString ^^ (_ => PLUS)
  def minus: Parser[Operation] = MINUS.toString ^^ (_ => MINUS)
  def multiply: Parser[Operation] = MULTIPLY.toString ^^ (_ => MULTIPLY)
  def divide: Parser[Operation] = DIVIDE.toString ^^ (_ => DIVIDE)
  def power: Parser[Operation] = POWER.toString ^^ (_ => POWER)

  def comma: Parser[Delimiter] = COMMA.toString ^^ (_ => COMMA)
  def leftParenthesis: Parser[Delimiter] = LEFT_PARENTHESIS.toString ^^ (_ => LEFT_PARENTHESIS)
  def rightParenthesis: Parser[Delimiter] = RIGHT_PARENTHESIS.toString ^^ (_ => RIGHT_PARENTHESIS)

  def number: Parser[NUMBER] = floatingPointNumber ^^ (x => NUMBER(x.toDouble))

  def variable: Parser[VAR_NAME] = "$" ~ ident ^^ {
    case _ ~ n => VAR_NAME(n)
  }

  def function: Parser[FUNC_NAME] = ident ^^ (n => FUNC_NAME(n))

  def tokens: Parser[List[MathExpression]] = {
    phrase(rep1(plus | power | multiply | divide |
      comma | leftParenthesis | rightParenthesis |
      number |
      minus |
      variable | function))
  }

  def apply(expression: String): List[MathExpression] =
    parse(tokens, expression) match {
      case NoSuccess(msg, next) => throw new IllegalArgumentException(msg)
      case Error(msg, next) => throw new IllegalArgumentException(msg)
      case Failure(msg, next) => throw new IllegalArgumentException(msg)
      case Success(result, next) => result
    }
}
