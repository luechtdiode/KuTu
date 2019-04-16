package ch.seidel.kutu.data

import java.sql.Date

import scala.reflect.runtime.universe._

object CaseObjectMetaUtil {
  val rm = reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)

  def toMap[T: TypeTag : reflect.ClassTag](instance: T) = {
    val im = rm.reflect(instance)
    getCaseMethods[T].collect {
      case m: MethodSymbol => m.name.decodedName.toString -> im.reflectMethod(m).apply()
    }.toMap
  }

  def copyWithValues[T: TypeTag : reflect.ClassTag](instance: T, values: Map[String, Any]): T = {
    if (values.isEmpty) instance else {
      val im = rm.reflect(instance)
      val mergedValues = typeOf[T].members.collect {
        case m: MethodSymbol if m.isCaseAccessor =>
          im.reflectMethod(m).apply() match {
            case value => values.get(m.name.decodedName.toString) match {
              case Some(newvalue) => m.name.decodedName.toString -> newvalue
              case None => m.name.decodedName.toString -> value
            }
          }
      }.toMap
      val (imn, _, applymethods) = getApplyMethod(typeOf[T].typeSymbol)
      val (applym, params) = applymethods.map { m => (m, m.paramLists.find(_.size == mergedValues.size)) }
        .find {
          case (_, Some(_)) => true
          case _ => false
        }
        .map { item =>
          val (method: MethodSymbol, paramslist: Option[List[Symbol]]) = item
          val iterable = paramslist match {
            case Some(list) => list.map(param => mergedValues(param.name.decodedName.toString))
            case _ => IndexedSeq.empty
          }
          (method, iterable.toIndexedSeq)
        }
        .head

      imn.reflectMethod(applym)(params: _*).asInstanceOf[T]
    }
  }

  type KeepLogicFn[T] = String => Option[(T, T) => T]

  def defaultKeepLogic[T: TypeTag : reflect.ClassTag](propertyname: String): Option[(T, T) => T] = {
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

  def mergeMissingProperties[T: TypeTag : reflect.ClassTag](keeping: T, toDelete: T, keepLogic: KeepLogicFn[Any] = defaultKeepLogic): T = {
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
          case (s1: String, s2: String) => if (s1.trim.length == 0) s2 else s1
          case (n1: Number, n2: Number) => if (n1.intValue() == 0) n2 else n1
          case (c1, c2) => if (c1.toString.compareTo(c2.toString) > 0) c1 else c2
        }
        case Some(fn) => fn(v1, v2)
      }
      (k, keepingValue)
    }

    CaseObjectMetaUtil.copyWithValues(keeping, mergegdMap)
  }

  def getCaseMethods[T: TypeTag]: Seq[MethodSymbol] = typeOf[T].members.collect {
    case m: MethodSymbol if m.isCaseAccessor => m
  }.toList

  private def getApplyMethod(sym: Symbol): (InstanceMirror, Type, List[MethodSymbol]) = {
    val instanceMirror = rm.reflect(rm.reflectModule(sym.asClass.companion.asModule).instance)
    val typeSignature = instanceMirror.symbol.typeSignature
    val applyMethods = typeSignature.member(TermName("apply")).alternatives.map(_.asMethod)

    (instanceMirror, typeSignature, applyMethods)
  }
}
