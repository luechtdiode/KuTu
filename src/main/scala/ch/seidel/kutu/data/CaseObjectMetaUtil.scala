package ch.seidel.kutu.data

import java.sql.Date
import scala.compiletime.*
import scala.deriving.*
import scala.quoted.*
import scala.reflect.runtime.universe.*

object CaseObjectMetaUtil {
  val rm = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)

  inline def toMap[T <: Product](instance: T)(using mirror: Mirror.ProductOf[T]): Map[String, Any] = {
    val fieldnames = getCaseFieldNames
    val fieldvalues = getCaseValues(instance)
    fieldnames.zip(fieldvalues).toMap
  }

  inline def getCaseFieldNames[T](using m: Mirror.ProductOf[T]): List[String] = constValue[m.MirroredElemLabels].toString.split(',').map(_.trim).toList

  inline def getCaseValues[T <: Product](instance: T)(using mirror: Mirror.ProductOf[T]): List[Any] = Tuple.fromProductTyped(instance).productIterator.toList

  inline def copyWithValues[T <: Product](instance: T, values: Map[String, Any])(using m: Mirror.ProductOf[T]): T = {
    if (values.isEmpty) instance else {
      val currentMap = toMap(instance)
      val mergedValues = currentMap.map { case (k, v) =>
        k -> values.getOrElse(k, v)
      }
      
      val elementLabels = constValue[m.MirroredElemLabels].toString.split(',').map(_.trim)
      val params = elementLabels.map(label => mergedValues(label))
      
      m.fromProduct(new Product {
        def canEqual(that: Any): Boolean = true
        def productArity: Int = params.length
        def productElement(n: Int): Any = params(n)
      })
    }
  }

  type KeepLogicFn[T] = String => Option[(T, T) => T]

  def defaultKeepLogic[T](propertyname: String): Option[(T, T) => T] = {
    propertyname match {
      case "id" => Some((v1, _) => v1)
      case "activ" => Some((v1, _) => v1)
      case "gebdat" => Some((v1, v2) => {
        v1 match {
          case Some(d: Date) =>
            if (!f"${d}%tF".endsWith("-01-01")) v1
            else v2 match {
              case Some(cd: Date) if (!f"${cd}%tF".endsWith("-01-01") || d.equals(cd)) =>
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

  def mergeMissingProperties[T <: Product](keeping: T, toDelete: T, keepLogic: KeepLogicFn[Any] = defaultKeepLogic)(using m: Mirror.ProductOf[T]): T = {
    val keepingAttributesMap: Map[String, Any] = toMap(keeping)
    val toDeletAttributesMap: Map[String, Any] = toMap(toDelete)
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

    copyWithValues(keeping, mergegdMap)
  }

/*
  inline def getCaseAccessors[T](using m: Mirror.ProductOf[T]): List[String] = 
    constValue[m.MirroredElemLabels].toString.split(',').map(_.trim).toList

  private def getApplyMethod(sym: Symbol): (InstanceMirror, Type, List[MethodSymbol]) = {
    val instanceMirror = rm.reflect(rm.reflectModule(sym.asClass.companion.asModule).instance)
    val typeSignature = instanceMirror.symbol.typeSignature
    val applyMethods = typeSignature.member(TermName("apply")).alternatives.map(_.asMethod)

    (instanceMirror, typeSignature, applyMethods)
  }

 */
}
