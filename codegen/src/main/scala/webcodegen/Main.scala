package webcodegen

import java.io.File

object Main {
  def main(args: Array[String]): Unit = {
    val _ = CodeGenerator.generate(
      WebComponentConfig(
        customElements = Seq(
          CustomElements(
            name = "shoelace",
            jsonFile = new File("node_modules/@shoelace-style/shoelace/dist/custom-elements.json"),
          ),
          CustomElements(
            name = "emojipicker",
            jsonFile = new File("node_modules/emoji-picker-element/custom-elements.json"),
          ),
          // CustomElements(
          //   name = "fast",
          //   jsonFile = new File("node_modules/@microsoft/fast-foundation/dist/custom-elements.json"),
          // ),
        )
      ),
      CodeGeneratorConfig(
        templates = Seq(
          Template.Outwatch
        ),
        packagePrefix = None,
        outDir = new File("web-components/"),
        scalafmt = true,
        scalaVersion = "3.4.1",
      ),
    )
  }

}
