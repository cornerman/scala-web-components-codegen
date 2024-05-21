package webcodegen.plugin

import sbt._
import sbt.Keys._
import webcodegen._

object WebCodegenPlugin extends AutoPlugin {
  override def trigger = noTrigger

  object autoImport {
    val webcodegenCustomElements =
      settingKey[Seq[CustomElements]]("The custom-element.json files to be processed")
    val webcodegenTemplates =
      settingKey[Seq[Template]]("The templates to be used in the code generator")
    val webcodegenScalafmt =
      settingKey[Boolean]("Whether to run scalafmt on the generated code")
    val webcodegenPackagePrefix =
      settingKey[Option[String]]("Package prefix for provided templates")
  }
  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    webcodegenCustomElements := Seq.empty,
    webcodegenTemplates      := Seq.empty,
    webcodegenPackagePrefix  := None,
    webcodegenScalafmt       := true,
    (Compile / sourceGenerators) += Def.task {
      val outDir = (Compile / sourceManaged).value / "scala" / "webcodegen"

      val generatedFiles = CodeGenerator.generate(
        WebComponentConfig(
          customElements = webcodegenCustomElements.value
        ),
        CodeGeneratorConfig(
          templates = webcodegenTemplates.value,
          packagePrefix = webcodegenPackagePrefix.value,
          outDir = outDir,
          scalafmt = webcodegenScalafmt.value,
          scalaVersion = scalaVersion.value,
        ),
      )

      generatedFiles.map(_.toFile)
    }.taskValue,
  )
}
