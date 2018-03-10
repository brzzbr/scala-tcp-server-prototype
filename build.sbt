lazy val `tcp-server`: Project = (project in file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    organization := "org.laborunion",
    name := "scala-tcp-server-prototype",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "2.12.4",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
    fork in run := true,
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= dependencies,
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    resolvers += Resolver.jcenterRepo
  )

lazy val dependencies: Seq[ModuleID] = {

  val akkaV = "2.5.11"
  val scalatestV = "3.0.5"
  val mockitoV = "2.15.0"

  Seq(
    // akka
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,

    // tests
    "org.scalatest" %% "scalatest" % scalatestV % "it, test",
    "org.mockito" % "mockito-core" % mockitoV % "it, test",

    // protobuf
    "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.42" % "protobuf"
  )
}
