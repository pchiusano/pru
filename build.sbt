organization := "pru"

name := "pru"

version := "0.3"

scalaVersion := "2.11.0"

crossScalaVersions := Seq("2.11.0", "2.10.4")

scalacOptions ++= {
  Seq("-encoding", "UTF-8", "-deprecation", "-unchecked",
      "-Xlint", "-Yno-adapted-args", "-feature",
      "-language:implicitConversions", "-language:higherKinds",
      "-language:existentials") ++
  (if (scalaVersion.value startsWith "2.10") Seq.empty
   else Seq("-Ywarn-unused", "-Ywarn-unused-import"))
}

libraryDependencies ++= {
  if (scalaVersion.value startsWith "2.10") Seq.empty
  else Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.1")
}

seq(bintraySettings:_*)

publishMavenStyle := true

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintray.Keys.packageLabels in bintray.Keys.bintray := Seq("XML", "JSON")
