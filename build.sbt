Global / onChangedBuildSource := ReloadOnSourceChanges

val scala_2_12 = "2.12.15"
val scala_2_13 = "2.13.6"

val baseSettings = Seq(
  crossScalaVersions := Seq(scala_2_12, scala_2_13),
  scalaVersion := scala_2_13,
  organization := "bondlink",
  version := "1.0.1",
  gitPublishDir := file("/src/maven-repo"),
)

lazy val nowarnPlugin = project.in(file("."))
  .settings(baseSettings)
  .settings(
    name := "nowarn-plugin",
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
  )

lazy val tests = project.in(file("tests"))
  .settings(baseSettings)
  .settings(
    publish := {},
    publishLocal := {},
    gitRelease := {},
    scalacOptions ++= {
      val jar = (nowarnPlugin / Compile / Keys.`package`).value
      Seq(
        s"-Xplugin:${jar.getAbsolutePath}",
        s"-Jdummy$name=${jar.lastModified}",
        "-P:nowarn:unused:msg=never used",
        "-P:nowarn:bl.unused:msg=never used",
        "-P:nowarn:undeprecated:cat=deprecation"
      )
    }
  )
  .aggregate(nowarnPlugin)
