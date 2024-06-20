package webcodegen

import webcodegen.model._

import scala.util.matching.Regex

object WebComponentsTranslator {

  val Def = WebComponentsDef
  val M   = CustomElementsManifest

  def fromManifest(customElements: CustomElements, manifest: CustomElementsManifest): WebComponentsDef = {
    val elements = manifest.modules.flatMap { module =>
      require(
        module.kind == "javascript-module",
        s"Unknown module type `${module.kind}` for module `${module.path}`.",
      )
      module.declarations.map { declaration =>
        require(
          declaration.kind == "class",
          s"Expected kind=class declaration in module `${module.path}`, got `${declaration.kind}` for declaration `${declaration.name}`.",
        )
        require(
          declaration.customElement,
          s"Expected customElement=true declaration in module `${module.path}` for declaration `${declaration.name}`.",
        )

        val props = allJsProperties(declaration)
        val attrs = attributes(declaration)
        Def.Element(
          tagName = declaration.tagName,
          name = declaration.name,
          importPath = customElements.jsImportPath(module.path),
          docUrl = declaration.documentation.filter(_.nonEmpty),
          description = (declaration.description.toVector ++ declaration.jsDoc
            .map(_.replaceAll("/\\*\\*|\\*/", "").replaceAll("\n \\* ?", "\n").trim)
            .toVector)
            .mkString("\n"),
          summary = declaration.summary.getOrElse(""),
          events = events(declaration),
          allJsProperties = props,
          attributes = attrs,
          cssProperties = cssProperties(declaration),
          cssParts = cssParts(declaration),
          slots = slots(declaration),
        )
      }
    }

    WebComponentsDef(elements)
  }

  def events(elementDeclaration: M.Declaration): Vector[Def.Event] = {
    elementDeclaration.events.map { event =>
      Def.Event(
        tpe = parseValueType(event.`type`, nameHint = event.eventName.getOrElse(event.name)),
        description = event.description.getOrElse(""),
        eventName = event.eventName,
        domName = event.name,
      )
    }
  }

  def allJsProperties(elementDeclaration: M.Declaration): Vector[Def.Member] = {
    elementDeclaration.members
      .filter(m => m.privacy.forall(_ == "public") && !m.static)
      .filter(m => Set("method", "field").contains(m.kind))
      .filterNot(_.`type`.exists(_.text.endsWith("Element"))) // TODO: better heuristic for slots
      .filterNot(_.`type`.exists(_.text.startsWith("Sl")))    // TODO: better heuristic for shoelace slots
      .flatMap { m =>
        val attrName = m.attribute.filter(_.nonEmpty)
        require(
          attrName.nonEmpty || !m.reflects,
          s"Reflected prop `${m.name}` in element `${elementDeclaration.tagName}` has no attribute specified.",
        )

        val probablyReflected =
          elementDeclaration.attributes.exists(attr => NameFormat.toCamelCase(attr.name) == NameFormat.toCamelCase(m.name))

        Some(
          Def.Member(
            propName = m.name,
            attrName = attrName,
            isReflected = m.reflects || probablyReflected,
            isMethod = m.kind == "method",
            isReadonly = m.readonly,
            tpe = parseValueType(m.`type`),
            default = m.default.filter(_.nonEmpty),
            description = m.description.getOrElse(""),
          )
        )
      }
  }

  def attributes(elementDeclaration: M.Declaration): Vector[Def.Attribute] = {
    elementDeclaration.attributes.flatMap { attr =>
      val tpe = parseValueType(attr.`type`)
      if (Def.Type.isHtmlCompatible(tpe)) {
        Some(
          Def.Attribute(
            attrName = attr.name,
            tpe = tpe,
            default = attr.default.filter(_.nonEmpty),
            description = attr.description.getOrElse(""),
          )
        )
      } else {
        // Attr contains HTML-incompatible types such as element, function, date, etc.
        // So, we must use it as a property instead.
        None
      }
    }
  }

  def isAttributeTypeHtmlCompatible(tpe: Def.Type): Boolean = tpe match {
    case Def.Type.Scalar(_) => true
    case _                  => false
  }

  def cssParts(elementDeclaration: M.Declaration): Vector[Def.CssPart] = {
    elementDeclaration.cssParts.map { part =>
      Def.CssPart(
        description = part.description.getOrElse(""),
        cssName = part.name,
      )
    }
  }

  def cssProperties(elementDeclaration: M.Declaration): Vector[Def.CssProperty] = {
    elementDeclaration.cssProperties.map { prop =>
      Def.CssProperty(
        description = prop.description.getOrElse(""),
        cssName = prop.name,
      )
    }
  }

  def slots(elementDeclaration: M.Declaration): Vector[Def.Slot] = {
    elementDeclaration.slots.map { slot =>
      Def.Slot(
        description = slot.description.getOrElse(""),
        slotName = slot.name,
      )
    }
  }

  // -- util helpers

  private val singleQuotedPattern: Regex = """^'([^']*)'$""".r
  private val objectBodyPattern: Regex   = """^\{(.*)\}$""".r
  private val arrayPattern: Regex        = """^(.*)\[\]$""".r

  private def parseValueType(value: Option[M.ValueType], nameHint: String = "Anon"): Def.Type =
    parseTypeString(value.fold("")(_.text), new NameProvider(nameHint))

  private def parseTypeString(typeString: String, nameHint: NameProvider): Def.Type = typeString match {
    case objectBodyPattern(body) =>
      val memberParts = body.split(",").map(_.trim).filter(_.nonEmpty)
      val memberTuples = memberParts.flatMap(_.split(":").toList match {
        case name :: tpe :: Nil => Some(name -> parseTypeString(tpe, nameHint))
        case _                  => None // TODO: error?
      })
      Def.Type.Struct(nameHint.newName(), memberTuples.toMap)
    case typeString if typeString.contains("|") =>
      val typeParts = typeString.split('|').map(_.trim).filter(_.nonEmpty)
      val types     = typeParts.map(parseTypeString(_, nameHint))
      types.toList match {
        case Nil           => Def.Type.None
        case single :: Nil => single
        case parts         => Def.Type.Union(parts.toVector)
      }
    case singleQuotedPattern(constant) => Def.Type.Scalar(s""""${constant}"""")
    case arrayPattern(tpe)             => Def.Type.Array(parseTypeString(tpe, nameHint))
    case "Undefined" | "undefined"     => Def.Type.Undefined
    case "Null" | "null"               => Def.Type.Null
    case "String" | "string"           => Def.Type.Scalar[String]
    case "Boolean" | "boolean"         => Def.Type.Scalar[Boolean]
    case "Number" | "number"           => Def.Type.Scalar[Double]
    case "CSSNumberish"                => Def.Type.Scalar[Double]
    case "Object"                      => Def.Type.Scalar("scala.scalajs.js.Object")
    case "Element"                     => Def.Type.Scalar("org.scalajs.dom.Element")
    case "HTMLElement"                 => Def.Type.Scalar("org.scalajs.dom.HTMLElement")
    case "SVGElement"                  => Def.Type.Scalar("org.scalajs.dom.SVGElement")
    case "MutationRecord"              => Def.Type.Scalar("org.scalajs.dom.MutationRecord")
    case "ResizeObserverEntry"         => Def.Type.Scalar("org.scalajs.dom.ResizeObserverEntry")
    case "MutationObserver"            => Def.Type.Scalar("org.scalajs.dom.MutationObserver")
    case ""                            => Def.Type.None
    case unknown                       => Def.Type.Unknown(unknown)
  }
}

private class NameProvider(baseName: String) {
  var newNameCnt = 0
  def newName(): String = {
    val postfix = if (newNameCnt == 0) "" else newNameCnt.toString
    newNameCnt += 1
    NameFormat.sanitizeScalaName(NameFormat.toPascalCase(baseName + postfix))
  }
}
