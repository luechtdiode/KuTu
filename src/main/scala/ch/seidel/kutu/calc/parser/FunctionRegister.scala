package ch.seidel.kutu.calc.parser

object FunctionRegister {
  def avg(a: Double, b: Double): Double = (a + b) / 2
  def avg(l: Array[Double]): Double = l.sum / l.length

  def sum(a: Double, b: Double): Double = a + b
  def sum(l: Array[Double]): Double = l.sum

  def min(l: Array[Double]): Double = l.min
  def max(l: Array[Double]): Double = l.max

  val function1: Map[String, Double => Double] = Map(
    // scala.math
    // Rounding
    "ceil" -> Math.ceil,
    "floor" -> Math.floor,
    "rint" -> Math.rint,
    "round" -> ((x: Double) => Math.round(x)).andThen(_.toDouble),
    // Exponential and Logarithmic
    "exp" -> Math.exp,
    "expm1" -> Math.expm1,
    "log" -> Math.log,
    "log10" -> Math.log10,
    "log1p" -> Math.log1p,
    // Trigonometric
    "acos" -> Math.acos,
    "asin" -> Math.asin,
    "atan" -> Math.atan,
    "cos" -> Math.cos,
    "sin" -> Math.sin,
    "tan" -> Math.tan,
    // Angular Measurement Conversion
    "toDegrees" -> Math.toDegrees,
    "toRadians" -> Math.toRadians,
    // Hyperbolic
    "cosh" -> Math.cosh,
    "sinh" -> Math.sinh,
    "tanh" -> Math.tanh,
    // Absolute Values
    "abs" -> Math.abs,
    // Signs
    "signum" -> Math.signum,
    // Root Extraction
    "cbrt" -> Math.cbrt,
    "sqrt" -> Math.sqrt,
    // Unit of Least Precision
    "ulp" -> Math.ulp
  )

  val function2: Map[String, (Double, Double) => Double] = Map(
    "+" -> (_ + _),
    "-" -> (_ - _),
    "*" -> (_ * _),
    "/" -> (_ / _),
    "avg" -> avg,
    "sum" -> sum,
    // scala.math
    // Minimum and Maximum
    "max" -> Math.max,
    "min" -> Math.min,
    // Exponential and Logarithmic
    "**" -> Math.pow,
    "pow" -> Math.pow,
    // Polar Coordinates
    "atan2" -> Math.atan2,
    "hypot" -> Math.hypot
  )
  val functionN: Map[String, Array[Double] => Double] = Map(
    "avg" -> avg,
    "sum" -> sum,
    "max" -> max,
    "min" -> min,
  )
}
