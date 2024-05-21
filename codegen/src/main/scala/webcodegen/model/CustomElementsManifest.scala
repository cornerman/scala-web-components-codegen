package webcodegen.model

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}

import scala.util.Try

// Taken from: https://github.com/raquo/laminar-shoelace-components/blob/f2894a247b197534e7a30f57287bbf475fc9a242/project/CustomElementsManifest.scala

case class CustomElementsManifest(
  schemaVersion: String, // "1.0.0"
  readme: String,        // ""
  modules: Vector[CustomElementsManifest.Module],
)
object CustomElementsManifest {

  case class Module(
    kind: String, // "javascript-module"
    path: String, // e.g. "components/alert/alert.js"
    declarations: Vector[Declaration],
    exports: Vector[Export],
  )

  case class Declaration(
    kind: String, // "class"
    name: String, // e.g. "SlAnimatedImage"
    description: Option[String],
    summary: Option[String],
    tagNameWithoutPrefix: Option[String], // e.g. "animated-image"
    tagName: String,                      // e.g. "sl-animated-image"
    customElement: Boolean,
    superclass: Superclass,
    jsDoc: Option[String],
    documentation: Option[String], // e.g. "https://shoelace.style/components/animated-image"
    status: Option[String],        // e.g. "stable"
    since: Option[String],         // e.g. "2.0"
    cssProperties: Vector[CssProperty],
    cssParts: Vector[CssPart],
    slots: Vector[Slot],
    members: Vector[Member],
    attributes: Vector[Attribute],
    events: Vector[Event],
    animations: Vector[Animation],
    dependencies: Vector[String], // e.g. ["sl-icon"]
  )

  case class CssProperty(
    description: Option[String],
    name: String, // e.g. "--border-color"
  )

  case class CssPart(
    description: Option[String],
    name: String,
  )

  case class Slot(
    description: Option[String],
    name: String,
  )

  // Method keys: kind, name, description, ...?
  case class Member(
    kind: String, // "field" | "method"
    name: String, // e.g. "dependencies" ????, "autoHideTimeout", ...
    description: Option[String],
    `type`: Option[ValueType],
    static: Boolean = false,
    parameters: Vector[MethodParameter], // only if kind == "method"
    attribute: Option[String],
    reflects: Boolean = false,
    readonly: Boolean = false,
    default: Option[String], // e.g. "'eager'", "''", "new LocalizeController(this)", or "" if not specified
    privacy: Option[String], // "public" | "private"
  )

  case class ValueType(
    text: String // "number" | "object" | "FocusOptions" (custom type example) | undefined | "'stringConstant'" | `"foo | bar" (union)` | "" (if none specified)
  )

  case class MethodParameter(
    name: String, // e.g. "options"
    optional: Boolean = false,
    `type`: Option[ValueType],
  )

  case class Event(
    `type`: Option[ValueType],
    description: Option[String],
    name: String,              // e.g. "sl-show"
    reactName: Option[String], // e.g. "onSlShow", or empty if not defined. I'm guessing it's a not-standard property.
    eventName: Option[String], // e.g. "SlShowEvent"
  )

  case class Attribute(
    name: String, // e.g. "formenctype"
    description: Option[String],
    `type`: Option[ValueType],
    resolveInitializer: Option[ResolveInitializer],
    default: Option[String],  // e.g. "'eager'", "''", "new LocalizeController(this)", or "" if not specified
    fieldName: Option[String],// e.g. "formEnctype"
  )

  case class ResolveInitializer(
    module: String // e.g. "src/components/animation/animation.component.ts", or empty if there is no resolve initializer.
  )

  case class Animation(
    name: String, // e.g. "alert.show"
    description: Option[String],
  )

  case class Superclass(
    name: String,          // e.g. "ShoelaceElement"
    module: Option[String],// e.g. "/src/internal/shoelace-element.js"
  )

  case class Export(
    kind: String, // "js"
    name: String, // e.g. "default"
    declaration: ExportDeclaration,
  )

  case class ExportDeclaration(
    name: String,          // e.g. "SlAnimatedImage"
    module: Option[String],// e.g. "components/animated-image/animated-image.js"
  )

  private val innerDecoder = JsonCodecMaker.make[ValueType]
  implicit val codecValueType: JsonValueCodec[ValueType] = new JsonValueCodec[ValueType] {
    override def decodeValue(in: JsonReader, default: ValueType): ValueType =
      Try(innerDecoder.decodeValue(in, default)).toOption.getOrElse {
        in.rollbackToken()
        ValueType(in.readString(null))
      }
    override def encodeValue(x: ValueType, out: JsonWriter): Unit = innerDecoder.encodeValue(x, out)
    override def nullValue: ValueType                             = null
  }
  implicit val codec: JsonValueCodec[CustomElementsManifest] = JsonCodecMaker.make
}
