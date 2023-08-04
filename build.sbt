Global / onChangedBuildSource := ReloadOnSourceChanges

val scala_2_13 = "2.13.11"
val scala_3 = "3.3.0"

def foldScalaV[A](scalaVersion: String)(_2: => A, _3: => A): A =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) => _2
    case Some((3, _)) => _3
  }

val baseSettings = Seq(
  crossScalaVersions := Seq(scala_2_13, scala_3),
  scalaVersion := scala_3,
  organization := "bondlink",
  version := "1.0.1",
  gitPublishDir := file("/src/maven-repo"),
)

lazy val nowarnPlugin = project.in(file("."))
  .settings(baseSettings)
  .settings(
    name := "nowarn-plugin",
    libraryDependencies += foldScalaV(scalaVersion.value)(
      "org.scala-lang" % "scala-compiler",
      "org.scala-lang" %% "scala3-compiler",
    ) % scalaVersion.value % "provided",
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
        s"-Jdummy${name.value}=${jar.lastModified}",
        // TODO - enable this once https://github.com/lampepfl/dotty/issues/18341 is resolved
        // "-Wunused:nowarn",
        "-P:nowarn:unused:msg=(unused|never used)",
        "-P:nowarn:bl.unused:msg=(unused|never used)",
        "-P:nowarn:undeprecated:cat=deprecation"
      ),
    }
  )
  .aggregate(nowarnPlugin)
