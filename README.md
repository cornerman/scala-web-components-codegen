# scala-web-components-codegen

A sbt-plugin and mill-plugin to generate scala code for web-components (`custom-elements.json`).

Heavily inspired by [laminar-shoelace-components](https://github.com/raquo/laminar-shoelace-components/). We aim to provide a more generic way to generate scala code for any web framework in scala.

> custom-elements.json + template => generate scala code

The plugin can be configured to parse a list of `custom-elements.json` files defining web-components.
You can provide a custom [scalate](https://scalate.github.io/scalate/) template to generate scala code out of this information (`webcodegen.Template.File(file)`).
Or you can chose an existing template that is included with the plugin (`webcodegen.Template.Outwatch`).


## Usage

### sbt

In `project/plugins.sbt`:
```sbt
addSbtPlugin("com.github.cornerman" % "sbt-web-components-codegen" % "0.1.0")
```

In `build.sbt`:
```sbt
lazy val web = project
  .enablePlugins(webcodegen.plugin.WebComponentsCodegenPlugin)
  .settings(
    // The custom-element.json files to be processed
    webcodegenCustomElementsJson := Map(
      "some-library" -> file("some/custom-elements.json"),
    )
    // The templates to be used in the code generator
    webcodegenTemplates := Seq(
      webcodegen.Template.Outwatch,
      webcodegen.Template.File(file("path/to/file.ssp")),
      webcodegen.Template.File(file("path/to/file.mustache")),
    )
  )
```

### mill

In `build.sc`:
```scala
import mill._, scalalib._
import $ivy.`com.github.cornerman::mill-web-components-codegen:0.1.0`, webcodegen.plugin.WebComponentsCodegenModule

object backend extends ScalaModule with WebComponentsCodegenModule {
  // The custom-element.json files to be processed
  def webcodegenCustomElementsJson = Map(
      "some-library" -> os.pwd / "custom-elements.json",
  )
  // The templates to be used in the code generator
  def webcodegenTemplates = Seq(
      webcodegen.Template.Outwatch,
      webcodegen.Template.File(os.pwd / "file.ssp"),
      webcodegen.Template.File(os.pwd / "file.mustache"),
  )
}
```

## Template

Templates can be configured by setting `webcodegenTemplates`.

We are using [scalate](https://scalate.github.io/scalate/) for templates, so you can use anything that is supported there (e.g. `mustache` or `ssp`) - the converter will be picked according to the file extension of the provided template file. Check the [scalate user guide](https://scalate.github.io/scalate/documentation/user-guide.html) for more details.

A template is called for each web-component found, and is passed an instance of [`webcodegen.WebComponent.Element`](codegen/src/main/scala/webcodegen/model/WebComponentsDef.scala) (variable name `element`) which contains all the extracted information.
You can see the declaration in the first line of each `ssp` template.

See [existing templates](codegen/src/main/resources/provided-templates/) for details.
