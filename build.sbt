import AssemblyKeys._ // put this at the top of the file


name := "SampleProject"

organization := "com.example"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.9.2"

resolvers ++= Seq(
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Spray Repository" at "http://repo.spray.cc/",
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases" at "http://oss.sonatype.org/content/repositories/releases"
  )


libraryDependencies ++= {
  Seq(
    "org.scalatest" %% "scalatest" % "1.9.1" % "test",
    "org.json4s" %% "json4s-native" % "3.2.2",
    "com.typesafe" % "config" % "0.3.1",
    "com.pi4j" % "pi4j-core" % "1.0-SNAPSHOT"
  )
}

assemblySettings


mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("org", "w3c", xs @ _*) => MergeStrategy.first
    case "about.html"     => MergeStrategy.discard
    case "log4j.properties"     => MergeStrategy.concat
    case x => old(x)
  }
}

mainClass in assembly := Some("com.example.Driver")
