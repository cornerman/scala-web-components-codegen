package webcodegen

import java.nio.file.Paths
import scala.jdk.CollectionConverters._

case class CustomElements(
  name: String,
  jsonFile: java.io.File,
  jsImportPath: String => String,
)

object CustomElements {
  def apply(
    name: String,
    jsonFile: java.io.File,
    basePath: Option[String] = None,
  ): CustomElements = {
    val basePathValue = basePath.getOrElse {
      // if custom-elements.json is relative to the javascript sources, this should work :|
      jsonFile.getParentFile.toPath.iterator.asScala.dropWhile(_.toString != "node_modules").drop(1).mkString("/")
    }
    CustomElements(
      name = name,
      jsonFile = jsonFile,
      jsImportPath = modulePath => Paths.get(basePathValue, modulePath.replaceFirst(".ts$", ".js")).toString,
    )
  }
}
