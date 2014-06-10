name := "jvmr"

organization := "org.ddahl"

version := "1.1.0"

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.10.4", "2.11.1")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "jline" % "jline" % "2.11",
  "org.scala-lang" % "jline" % "2.11.0-M3"
)
