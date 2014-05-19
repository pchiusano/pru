# Pru #

Frustrated by bloated, slow, and overly complicated XML and JSON writing libraries? Pru is the simplest thing that could possibly work--a small ([less than 100 lines](https://github.com/pchiusano/pru/blob/master/src/main/scala/Writer.scala)) collection of combinators that build up a `StringBuilder => Unit` (newtyped as `Action`). It has zero dependencies. Here's a simple example of its use:

``` Scala
import pru.write.{JSON => J, XML => X, Writer => W}

val xml = X.Nest("foo") { X.LeafTag("bar", "id" -> "1", "bar" -> "2") }

val json = J.List(
  J.literal("uno"),
  J.literal("dos"),
  J.literal("tres")
)

val xml2 = xml ++ xml
val json2 = J.List(json, json)

println(xml)
println(xml2)
println(json2)
```

This produces:

    <foo><bar id="1" bar="2"/></foo>
    <foo><bar id="1" bar="2"/></foo><foo><bar id="1" bar="2"/></foo>
    [["uno", "dos", "tres"], ["uno", "dos", "tres"]]

By convention, functions starting with a capital letter are variadic (for instance, `J.List` above), while function starting with a lowercase letter of the same name accept an ordinary `Seq` input.

#### Things this library does not do ####

* _Pretty-printing:_ Everything goes on one line.
* _Produce XML or JSON documents that don't fit in memory:_ You are building up a function which updates a `StringBuilder`, so streaming a huge document is not recommended. Of course, you can use this library to produce individual chunks of a large stream.
* _Implicit derivation of XML or JSON serialization code:_ There is no typeclass magic whatsoever, just regular combinators, invoked explicitly. Nothing stops you from building up a collection of `implicit` `A => Action` values if you're so inclined.

## Where to get it ##

Add the following to your sbt build:

``` Scala
resolvers += "Bintray Repo" at "http://dl.bintray.com/pchiusano/maven"

libraryDependencies += "pru" %% "pru" % "0.4"
```

There are builds against Scala 2.10.3 and Scala 2.11.

## Documentation ##

Just [read the source](https://github.com/pchiusano/pru/blob/master/src/main/scala/Writer.scala). It's less than 100 lines. The type signatures and a REPL session should tell all you need to know!
