package webcodegen

import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
import org.fusesource.scalate.{TemplateEngine, TemplateSource}
import org.scalafmt.Scalafmt
import org.scalafmt.config.ScalafmtConfig
import webcodegen.model.{CustomElementsManifest, WebComponentsDef}

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.io.Source

import scala.meta.dialects

case class CustomElementsDefinition(
  customElementsJson: CustomElements,
  manifest: CustomElementsManifest,
  components: WebComponentsDef,
)

case class WebComponentConfig(
  customElements: Seq[CustomElements]
)

case class CodeGeneratorConfig(
  templates: Seq[Template],
  packagePrefix: Option[String],
  outDir: File,
  scalafmt: Boolean,
  scalaVersion: String,
)

object CodeGenerator {
  def generate(web: WebComponentConfig, config: CodeGeneratorConfig): Seq[Path] = {
    // gather web components

    val definitions = web.customElements.map { customElements =>
      val json       = Files.readString(customElements.jsonFile.toPath)
      val manifest   = readFromString[CustomElementsManifest](json)
      val components = WebComponentsTranslator.fromManifest(customElements, manifest)
      CustomElementsDefinition(customElements, manifest, components)
    }

    // scalate

    val templateEngine = new TemplateEngine()
    templateEngine.escapeMarkup = false

    val templateSources = config.templates.map {
      case Template.Resource(resource) => TemplateSource.fromSource(resource, Source.fromResource(resource, this.getClass.getClassLoader))
      case Template.File(file)         => TemplateSource.fromFile(file)
    }

    // run templates on web components

    definitions.flatMap { definition =>
      definition.components.elements.flatMap { element =>
        val data = Map(
          "packagePrefix" -> config.packagePrefix.getOrElse("webcodegen"),
          "group"         -> definition.customElementsJson.name,
          "element"       -> element,
        )

        templateSources.map { templateSource =>
          val rawOutput = templateEngine.layout(templateSource, data)

          val formatted = if (config.scalafmt) {
            val scalafmtConfig = ScalafmtConfig.default
              .withDialect(scalaVersionToScalafmtDialect(config.scalaVersion))
              .copy(maxColumn = 140)
            Scalafmt.format(rawOutput, scalafmtConfig).toEither.toOption
          } else None

          val output = formatted.getOrElse(rawOutput)
          val outputPath = Paths.get(
            config.outDir.getPath,
            templateSource.uri,
            definition.customElementsJson.name,
            s"${element.scalaName.capitalize}.scala",
          )

          Files.createDirectories(outputPath.getParent)
          Files.write(outputPath, output.getBytes)

          outputPath
        }
      }
    }.toSeq
  }

  def scalaVersionToScalafmtDialect(scalaVersion: String) = scalaVersion match {
    case v if v.startsWith("3.") => dialects.Scala3
    case _                       => dialects.Scala
  }
}
