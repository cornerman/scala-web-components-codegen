package webcodegen.plugin

import mill._
import scalalib._
import webcodegen._

trait WebCodegenModule extends ScalaModule {

  // The custom-element.json files to be processed
  def webcodegenCustomElements: Seq[CustomElements] = Seq.empty
  // The templates to be used in the code generator
  def webcodegenTemplates: Seq[Template] = Seq.empty
  // Whether to run scalafmt on the generated code
  def webcodegenScalafmt: Boolean = true
  // Whether to run scalafmt on the generated code
  def webcodegenPackagePrefix: Option[String] = None
  // Output path for the generated code
  def webcodegenOutPath: T[PathRef] = T { PathRef(T.ctx().dest / "scala") }

  def webcodegenTask: Task[Seq[PathRef]] = T.task {

    val generatedFiles = CodeGenerator.generate(
      WebComponentConfig(
        customElements = webcodegenCustomElements
      ),
      CodeGeneratorConfig(
        templates = webcodegenTemplates,
        outDir = webcodegenOutPath().path.toIO,
        packagePrefix = webcodegenPackagePrefix,
        scalafmt = webcodegenScalafmt,
        scalaVersion = scalaVersion(),
      ),
    )

    generatedFiles.map(f => PathRef(os.Path(f.toFile)))
  }

  override def generatedSources: T[Seq[PathRef]] = T {
    val scalaOutput = webcodegenTask()
    scalaOutput ++ super.generatedSources()
  }
}
