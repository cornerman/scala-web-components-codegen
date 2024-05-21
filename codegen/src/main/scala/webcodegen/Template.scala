package webcodegen

sealed trait Template
object Template {
  case class Resource(resource: String) extends Template
  case class File(file: java.io.File)   extends Template

  val Outwatch: Template = Resource("provided-templates/outwatch.ssp")
}
