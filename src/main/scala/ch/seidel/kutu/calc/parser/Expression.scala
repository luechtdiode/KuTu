package ch.seidel.kutu.calc.parser

case class Expression[A, B](eval: A => B) {
  def map2[C, D](that: Expression[A, C])(g: (B, C) => D): Expression[A, D] =
    Expression(x => g(this.eval(x), that.eval(x)))

  def map[C](g: B => C): Expression[A, C] = Expression(g compose eval)
}

object Expression {
  import FunctionRegister.*

  def apply(ast: MathExpAST): Expression[String => Double, Double] = ast match {
    case Constant(d: Double) => Expression(_ => d)
    case c @ Constant(_) => throw new IllegalArgumentException(c.toString)
    case Variable(n) => Expression(f => f(n))
    case Operator2(op, v1, v2) =>
      Expression(v1).map2(Expression(v2))(function2(op))
    case f @ OperatorN(op, as) =>
      val args = sequence(as.map(Expression.apply)).map(_.toArray)
      args.map{ (xs: Array[Double]) =>
        xs.length match {
          case 1 => function1(op)(xs.head)
          case 2 => function2(op)(xs.head, xs(1))
          case n if n > 0 => functionN(op)(xs)
          case _ => throw new UnsupportedOperationException(f.toString)
        }
      }
  }

  private def unit[A, B](b: => B): Expression[A, B] = Expression(_ => b)

  def sequence[A, B](ls: List[Expression[A, B]]): Expression[A, List[B]] =
    ls.foldRight(unit[A, List[B]](List.empty))(
      (e, acc) => Expression(x => e.eval(x) :: acc.eval(x)))
}