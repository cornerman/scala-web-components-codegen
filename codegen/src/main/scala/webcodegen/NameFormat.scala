package webcodegen

object NameFormat {

  private val scalaKeywords = {
    val st = scala.reflect.runtime.universe.asInstanceOf[scala.reflect.internal.SymbolTable]
    st.nme.keywords.map(_.toString)
  }

  def joinScalaNames(head: String, tails: String*): String = {
    val parts = head +: tails.map(_.replaceAll("`", "").capitalize)
    val name  = parts.mkString("")
    sanitizeScalaName(name)
  }

  def sanitizeScalaName(rawName: String): String = {
    val name              = rawName.trim.replaceAll("`", "")
    def isValidIdentifier = name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")
    if (!scalaKeywords(name) && isValidIdentifier) name else s"`$name`"
  }

  def toCamelCase(str: String): String  = caseConvert(_.toLowerCase, _.toLowerCase.capitalize, "", str)
  def toPascalCase(str: String): String = caseConvert(_.toLowerCase.capitalize, _.toLowerCase.capitalize, "", str)

  // Taken from: https://github.com/process-street/scala-encase
  private val caseSeparatorPattern = List(
    "\\s+",
    "_",
    "-",
    "(?<=[A-Z])(?=[A-Z][a-z])",
    "(?<=[^A-Z_-])(?=[A-Z])",
    "(?<=[A-Za-z])(?=[^A-Za-z])",
  ).mkString("|").r

  private def caseConvert(headTransform: String => String, tailTransform: String => String, sep: String, str: String): String = {
    val split  = caseSeparatorPattern.split(str)
    val result = split.take(1).map(headTransform) ++ split.drop(1).map(tailTransform)
    result.mkString(sep)
  }
}
