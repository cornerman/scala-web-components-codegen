Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  Seq(
    organization := "com.github.cornerman",
    licenses     := Seq("MIT License" -> url("https://opensource.org/licenses/MIT")),
    homepage     := Some(url("https://github.com/cornerman/scala-web-components-codegen")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/cornerman/scala-web-components-codegen"),
        "scm:git:git@github.com:cornerman/scala-web-components-codegen.git",
        Some("scm:git:git@github.com:cornerman/scala-web-components-codegen.git"),
      )
    ),
    pomExtra :=
      <developers>
      <developer>
        <id>jkaroff</id>
        <name>Johannes Karoff</name>
        <url>https://github.com/cornerman</url>
      </developer>
    </developers>,
  )
)

// TODO: Use sbt-cross to workaround: https://github.com/sbt/sbt/issues/5586
lazy val codegen = project
  .settings(
    name := "scala-web-components-codegen",
    libraryDependencies ++= Seq(
      "org.scala-lang"                         % "scala-reflect"         % scalaVersion.value,
      "org.scalatra.scalate"                  %% "scalate-core"          % "1.10.1",
      "org.scalameta"                         %% "scalafmt-core"         % "3.8.1",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "2.28.5",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.28.5" % "compile-internal",
    ),
  )
  .cross

lazy val codegen212 = codegen("2.12.19")
lazy val codegen213 = codegen("2.13.13")

lazy val cli = project
  .settings(
    name               := "scala-web-components-codegen-cli",
    scalaVersion       := "2.13.13",
    crossScalaVersions := Seq("2.13.13"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "mainargs" % "0.7.1"
    ),
  )
  .dependsOn(codegen213)

lazy val pluginSbt = project
  .settings(
    name               := "sbt-web-components-codegen",
    scalaVersion       := "2.12.19",
    crossScalaVersions := Seq("2.12.19"),
    sbtPlugin          := true,
    publishMavenStyle  := true,
  )
  .dependsOn(codegen212)

lazy val pluginMill = project
  .settings(
    name               := "mill-web-components-codegen",
    scalaVersion       := "2.13.13",
    crossScalaVersions := Seq("2.13.13"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "mill-main"     % "0.11.7" % Provided,
      "com.lihaoyi" %% "mill-main-api" % "0.11.7" % Provided,
      "com.lihaoyi" %% "mill-scalalib" % "0.11.7" % Provided,
    ),
  )
  .dependsOn(codegen213)
