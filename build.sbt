import Dependencies._

val PixelBattleApi = Project("scala-tcp-server-prototype", file("."))
  .enablePlugins(DockerSupport)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(
    inThisBuild(List(
      organization := "org.laborunion",
      scalaVersion := "2.12.4"
    )),
    name := "CosmoSim",
    libraryDependencies ++=
      loggingDependencies ++
        akkaDependencies ++
        commonDependencies ++
        domainDependencies ++
        testDependencies,
    externalResolvers := Dependencies.libsResolvers,
    scalacOptions ++= Seq(
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Ywarn-dead-code",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "utf8"
    ),
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    parallelExecution in Test := false)

addCommandAlias("build", "; clean; test; package")
addCommandAlias("build-docker-local", "; clean; test; package; docker:publishLocal")
addCommandAlias("build-docker-remote", "; clean; test; package; docker:publish")