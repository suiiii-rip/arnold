import com.typesafe.sbt.packager.docker.DockerVersion

val versions = new {
  val zio = "1.0.3"
  val zioInteropCats = "2.2.0.1"
//  val zioInteropReactivestreams = "1.3.0.7-2"
  val zioLogging = "0.5.4"
  val zioConfig = "1.0.0-RC31-1"

  val http4s = "0.21.14"
  val circe = "0.13.0"
  val logback = "1.2.3"
  val reactivestreams = "1.0.3"

  val scalatest = "3.2.3"
  val scalatic = "3.2.2"

  val scala = "2.13.4"
}

val dependencies = {
  import versions._
  new {
    val zio = "dev.zio" %% "zio" % versions.zio
    val `zio-streams` = "dev.zio" %% "zio-streams" % versions.zio
    val `zio-interop-cats` = "dev.zio" %% "zio-interop-cats" % zioInteropCats
//    val `zio-interop-reactivestreams` = "dev.zio" %% "zio-interop-reactivestreams" % zioInteropReactivestreams
    val `zio-logging` = "dev.zio" %% "zio-logging" % zioLogging
    val `zio-logging-slf4j` = "dev.zio" %% "zio-logging-slf4j" % zioLogging

    val `zio-config` = "dev.zio" %% "zio-config" % zioConfig
    val `zio-config-magnolia` = "dev.zio" %% "zio-config-magnolia" % zioConfig

    val `http4s-core` = "org.http4s" %% "http4s-core" % http4s
    val `http4s-server` = "org.http4s" %% "http4s-server" % http4s
    val `http4s-blaze-server` = "org.http4s" %% "http4s-blaze-server" % http4s
    val `http4s-circe` = "org.http4s" %% "http4s-circe" % http4s
    val `http4s-dsl` = "org.http4s" %% "http4s-dsl" % http4s

    val `circe-core` = "io.circe" %% "circe-core" % circe
    val `circe-generic` = "io.circe" %% "circe-generic" % circe

    val `logback-classic` = "ch.qos.logback" % "logback-classic" % logback
//    val `reactive-streams` = "org.reactivestreams" % "reactive-streams" % reactivestreams

    val scalatest = "org.scalatest" %% "scalatest" % versions.scalatest % "test"
    val scalatic = "org.scalactic" %% "scalactic" % versions.scalatic
  }
}

val commonSettings = Seq(
  organization := "rip.suiiii",
  version := "1.0.0-SNAPSHOT",
  scalaVersion := versions.scala,
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
  dependencyOverrides ++= {
    import dependencies._
    Seq(
      scalatest,
    )
  },
  dockerVersion := Some(DockerVersion(19, 3, 13, Some("ce"))),
  dockerBaseImage := "registry.hub.docker.com/library/openjdk:11",
)

lazy val core = Project(
  id = "arnold-core",
  base = file("core")
)
  .settings(
    libraryDependencies ++= {
      import dependencies._
      Seq(
        `logback-classic`,
      )
    },
    unusedCompileDependenciesFilter -= moduleFilter("ch.qos.logback", "logback-classic"),
  )
  .settings(commonSettings: _*)
  .enablePlugins(ReproducibleBuildsPlugin)


lazy val app = Project(
  id = "arnold-app",
  base = file("app")
)
  .settings(
    libraryDependencies ++= {
      import dependencies._
      Seq(
        zio,
        `zio-streams`,
        `zio-interop-cats`,
        `zio-logging`,
        `zio-logging-slf4j`,
        `zio-config`,
        `zio-config-magnolia`,

        `http4s-core`,
        `http4s-server`,
        `http4s-blaze-server`,
        `http4s-circe`,
        `http4s-dsl`,

        `circe-core`,
        `circe-generic`,

      )
    },
    undeclaredCompileDependenciesFilter -= moduleFilter("co.fs2", "fs2-core"),
    undeclaredCompileDependenciesFilter -= moduleFilter("com.chuusai", "shapeless"),
    undeclaredCompileDependenciesFilter -= moduleFilter("dev.zio", "izumi-reflect"),
    undeclaredCompileDependenciesFilter -= moduleFilter("org.typelevel", "cats-core"),
    undeclaredCompileDependenciesFilter -= moduleFilter("org.typelevel", "cats-effect"),
    unusedCompileDependenciesFilter -= moduleFilter("ch.qos.logback", "logback-classic"),
    crossPaths := false,

    dockerExposedPorts ++= Seq(48080),
  )
  .settings(commonSettings: _*)
  .enablePlugins(ReproducibleBuildsPlugin, JavaAppPackaging, UniversalDeployPlugin, DockerPlugin)
  .dependsOn(core)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(core, app)
