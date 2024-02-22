Global / onChangedBuildSource := ReloadOnSourceChanges

val scala3 = "3.3.1"
val scalaVersions = Seq("2.13.12", scala3)

ThisBuild / crossScalaVersions := scalaVersions

// GitHub Actions config
val javaVersions = Seq(8, 11, 17, 21).map(v => JavaSpec.temurin(v.toString))

ThisBuild / githubWorkflowJavaVersions := javaVersions
ThisBuild / githubWorkflowArtifactUpload := false
ThisBuild / githubWorkflowBuildMatrixFailFast := Some(false)
ThisBuild / githubWorkflowTargetBranches := Seq("main")
ThisBuild / githubWorkflowPublishTargetBranches := Seq()

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("tests/compile"), name = Some("test")),
)

def foldScalaV[A](scalaVersion: String)(_2: => A, _3: => A): A =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) => _2
    case Some((3, _)) => _3
  }

val baseSettings = Seq(
  crossScalaVersions := scalaVersions,
  scalaVersion := scala3,
  organization := "bondlink",
  version := "1.1.1",
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
