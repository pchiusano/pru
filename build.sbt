organization := "pru"

name := "pru"

version := "0.3"

scalaVersion := "2.10.0"

seq(bintraySettings:_*)

publishMavenStyle := true

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintray.Keys.packageLabels in bintray.Keys.bintray := Seq("XML", "JSON")
