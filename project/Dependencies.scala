import Dependencies._
import sbt.{ModuleID, _}

object Dependencies {
  val libsResolvers = Seq(
    "JCenter artifactory" at "https://jcenter.bintray.com",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  )

  object Versions {
    val logbackVersion = "1.2.3"
    val scalaLoggingVersion = "3.5.0"
    val akkaHttpVersion = "10.1.0"
    val akkaVersion = "2.5.11"
    val akkaHttpCorsVersion = "0.2.2"
    val scalaJavaCompatVersion = "0.8.0"
    val scalaTestVersion = "3.0.4"
    val mockitoVersion = "2.8.9"
    val pegdownVersion = "1.6.0"
    val protobufVersion = "0.7.1"
  }

  private lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logbackVersion
  private lazy val logging = "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLoggingVersion

  private lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % Versions.akkaHttpVersion
  private lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % Versions.akkaVersion
  private lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % Versions.akkaVersion
  private lazy val akkaTyped = "com.typesafe.akka" %% "akka-typed" % Versions.akkaVersion
  private lazy val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Versions.akkaVersion
  private lazy val akkaHttpCors = "ch.megard" %% "akka-http-cors" % Versions.akkaHttpCorsVersion

  private lazy val scalaJavaCompat = "org.scala-lang.modules" %% "scala-java8-compat" % Versions.scalaJavaCompatVersion

  private lazy val scalaPbRuntime = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

  private lazy val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Versions.akkaVersion
  private lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTestVersion
  private lazy val mockito = "org.mockito" % "mockito-core" % Versions.mockitoVersion
  private lazy val pegdown = "org.pegdown" % "pegdown" % Versions.pegdownVersion

  lazy val loggingDependencies = Seq(logging, logback)
  lazy val akkaDependencies = Seq(akkaHttp, akkaStream, akkaActor, akkaSlf4j, akkaHttpCors)
  lazy val commonDependencies = Seq(scalaJavaCompat)

  lazy val domainDependencies = Seq(scalaPbRuntime)

  lazy val testDependencies: Seq[ModuleID] = {
    Seq(akkaTestKit, scalaTest, mockito, pegdown) map (_ % Test)
  }
}
