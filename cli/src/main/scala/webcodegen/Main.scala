package webcodegen

import mainargs.{arg, main, Flag, ParserForMethods}

import java.io.File

object Main {
  @main
  def run(
    @arg(doc = "Output path for the generated code")
    outDir: String,
    @arg(doc = "Name of the custom-elements library to process")
    customElementsName: String,
    @arg(doc = "Path to the custom-elements.json file to process")
    customElementsJson: String,
    @arg(doc = "Use packaged Outwatch template for the code generator")
    templateOutwatch: Flag,
    @arg(doc = "A template file for the code generator")
    templateFile: Seq[String],
    @arg(doc = "Package prefix used inside the generated code")
    packagePrefix: Option[String],
    @arg(doc = "Scala version to format the code with scalafmt")
    scalaVersion: Option[String],
  ) = {
    val webConfig = WebComponentConfig(
      customElements = Seq(
        CustomElements(
          name = customElementsName,
          jsonFile = new File(customElementsJson),
          basePath = Some("."),
        )
      )
    )

    val codeGeneratorConfig = CodeGeneratorConfig(
      templates = templateFile.map(s => Template.File(new File(s))) ++ Option.when(templateOutwatch.value)(Template.Outwatch),
      packagePrefix = packagePrefix,
      outDir = new File(outDir),
      scalafmt = scalaVersion.isDefined,
      scalaVersion = scalaVersion.getOrElse("3.0.0"),
    )

    val _ = CodeGenerator.generate(webConfig, codeGeneratorConfig)
  }

  def main(args: Array[String]): Unit = {
    val _ = ParserForMethods(this).runOrExit(args.toSeq)
  }
}
