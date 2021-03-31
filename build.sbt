Global / onChangedBuildSource := ReloadOnSourceChanges

val baseSettings = Seq(
  scalaVersion := "2.13.5",
  organization := "bondlink",
  version := "1.0.0",
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
