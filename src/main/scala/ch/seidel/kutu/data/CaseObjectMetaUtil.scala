package ch.seidel.kutu.data

import java.sql.Date
import scala.compiletime.*
import scala.deriving.*
import scala.quoted.*

inline def labels[A](using m: Mirror.ProductOf[A]) =
  constValueTuple[m.MirroredElemLabels].asInstanceOf[Product].productIterator.map(_.toString).toList

inline def caseClassToMap[A](a: A)(using m: Mirror.ProductOf[A]): Map[String, Any] =
  val lbls = labels[A]
  val vals  = a.asInstanceOf[Product].productIterator.toList
  lbls.zip(vals).toMap

inline def getCaseValues[T <: Product](instance: T)(using mirror: Mirror.ProductOf[T]): List[Any] = Tuple.fromProductTyped(instance).productIterator.toList

inline def mapToCaseClass[A](map: Map[String, Any])(using m: Mirror.ProductOf[A]): A =
  val lbls = labels[A]
  val vals = lbls.map(name => map.getOrElse(name, throw new IllegalArgumentException(s"Missing field: $name")))
  m.fromProduct(Tuple.fromArray(vals.toArray))

inline def copyToCaseClass[A](instance: A, map: Map[String, Any])(using m: Mirror.ProductOf[A]): A =
  val lbls = labels[A]
  val instanceMap = caseClassToMap[A](instance)
  val vals = lbls.map(name => map.getOrElse(name, instanceMap(name)))
  m.fromProduct(Tuple.fromArray(vals.toArray))

type KeepLogicFn[T] = String => Option[(T, T) => T]

def defaultKeepLogic[T](propertyname: String): Option[(T, T) => T] = {
  propertyname match {
    case "id" => Some((v1, _) => v1)
    case "activ" => Some((v1, _) => v1)
    case "gebdat" => Some((v1, v2) => {
      v1 match {
        case Some(d: Date) =>
          if (!f"$d%tF".endsWith("-01-01")) v1
          else v2 match {
            case Some(cd: Date) if (!f"$cd%tF".endsWith("-01-01") || d.equals(cd)) =>
              v2
            case _ =>
              v1
          }
        case None =>
          v2
      }
    })
    case _ => None
  }
}

inline def mergeMissingProperties[T <: Product](keeping: T, toDelete: T, keepLogic: KeepLogicFn[Any] = defaultKeepLogic)(using m: Mirror.ProductOf[T]): T = {
  val keepingAttributesMap: Map[String, Any] = caseClassToMap(keeping)
  val toDeletAttributesMap: Map[String, Any] = caseClassToMap(toDelete)
  val mergegdMap: Map[String, Any] = keepingAttributesMap.map { pair =>
    val (k, v1) = pair
    val v2 = toDeletAttributesMap(k)
    val keepingValue = keepLogic(k) match {
      case None => (v1, v2) match {
        case (Some(o1), Some(o2)) => if (o1.toString.compareTo(o2.toString) > 0) Some(o1) else Some(o2)
        case (Some(o1), None) => Some(o1)
        case (None, Some(o2)) => Some(o2)
        case (None, None) => None
        case (b1: Boolean, b2: Boolean) => b1 || b2
        case (s1: String, s2: String) => if (s1.trim.isEmpty) s2 else s1
        case (n1: Number, n2: Number) => if (n1.intValue() == 0) n2 else n1
        case (c1, c2) => if (c1.toString.compareTo(c2.toString) > 0) c1 else c2
      }
      case Some(fn) => fn(v1, v2)
    }
    (k, keepingValue)
  }

  mapToCaseClass(mergegdMap)
}
