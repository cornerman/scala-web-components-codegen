package webcodegen.model

import webcodegen.NameFormat

import scala.reflect.{classTag, ClassTag}

case class WebComponentsDef(
  elements: Vector[WebComponentsDef.Element]
)

object WebComponentsDef {

  case class Element(
    tagName: String,
    name: String,
    importPath: String, // e.g. "components/button/button.js"
    description: String,
    summary: String,
    docUrl: Option[String],
    events: Vector[Event],
    allJsProperties: Vector[Member],
    attributes: Vector[Attribute],
    cssProperties: Vector[CssProperty],
    cssParts: Vector[CssPart],
    slots: Vector[Slot],
  ) {
    def scalaName = NameFormat.sanitizeScalaName(NameFormat.toCamelCase(name))
  }

  case class Event(
    description: String,
    domName: String,
    eventName: Option[String],
    tpe: Type,
  ) {
    def scalaName = NameFormat.sanitizeScalaName(NameFormat.toCamelCase(domName))
  }

  case class Member(
    propName: String,
    attrName: Option[String],
    isReflected: Boolean,
    isMethod: Boolean,
    isReadonly: Boolean,
    tpe: Type,
    default: Option[String],
    description: String,
  ) {
    def scalaName = NameFormat.sanitizeScalaName(NameFormat.toCamelCase(propName))
  }

  case class Attribute(
    attrName: String,
    tpe: Type,
    default: Option[String],
    description: String,
  ) {
    def scalaName = NameFormat.sanitizeScalaName(NameFormat.toCamelCase(attrName))
  }

  case class CssProperty(
    description: String,
    cssName: String, // e.g. "--border-color"
  ) {
    def scalaName = NameFormat.sanitizeScalaName(NameFormat.toCamelCase(cssName.stripPrefix("--")))
  }

  case class CssPart(
    description: String,
    cssName: String,
  ) {
    def scalaName = NameFormat.sanitizeScalaName(NameFormat.toCamelCase(cssName))
  }

  case class Slot(
    description: String,
    slotName: String,
  ) {
    def isDefault = slotName.isEmpty
    def scalaName = if (isDefault) "default" else NameFormat.sanitizeScalaName(NameFormat.toCamelCase(slotName))
  }

  sealed trait Type {
    final def scalaType(orElse: => String): String = Type.toScalaType(this, orElse)
  }
  object Type {
    case object Null                                                     extends Type
    case object Undefined                                                extends Type
    case class Union(types: Vector[Type])                                extends Type
    case class Array(inner: Type)                                        extends Type
    case object None                                                     extends Type
    case class Unknown(original: String)                                 extends Type
    case class Struct(scalaTypeName: String, members: Map[String, Type]) extends Type
    case class Scalar(scalaTypeName: String)                             extends Type
    object Scalar {
      def apply[T: ClassTag]: Scalar = Scalar(classTag[T].toString().replaceFirst("java\\.lang\\.", ""))
    }

    def toScalaType(tpe: Type, orElse: => String): String = tpe match {
      case Null         => "Null"
      case Undefined    => "Unit"
      case Union(types) =>
        // TODO: scala 2 (minimum we need import scala.scalajs.js.|)
        // TODO: should we remove nulls from type union?
        // TODO: should we rewrite undefined in union to js.UndefOr[rest]?
        types.map(_.scalaType(orElse)).mkString(" | ")
      case Array(inner)             => s"scala.scalajs.js.Array[${inner.scalaType(orElse)}]"
      case None                     => orElse
      case Unknown(original)        => s"${orElse} /* $original */"
      case Struct(scalaTypeName, _) => scalaTypeName
      case Scalar(scalaTypeName)    => scalaTypeName
    }

    def asHtmlCompatible(tpe: Type): Type = tpe match {
      case Scalar("Boolean") => tpe
      case _                 => Scalar("String")
    }
  }
}
