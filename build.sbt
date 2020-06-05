name := """playnevmas"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.2"

PlayKeys.devSettings += "play.server.http.port" -> "80"

libraryDependencies += guice
libraryDependencies += javaJdbc
libraryDependencies += "org.hibernate" % "hibernate-core" % "5.4.2.Final"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.12"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.4"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.4"
// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.11.0"
// https://mvnrepository.com/artifact/org.json/json
libraryDependencies += "org.json" % "json" % "20200518"

libraryDependencies += javaJpa

