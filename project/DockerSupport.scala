import com.typesafe.sbt.SbtNativePackager.{Docker, Universal}
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import sbt.Keys.{javaOptions, mainClass}
import sbt.{Def, _}

object DockerSupport extends AutoPlugin {
  override def requires: JavaServerAppPackaging.type = JavaServerAppPackaging
  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    mainClass in Compile := Some("org.laborunion.project.hollyshit.Runner"),
    dockerBaseImage := "anapsix/alpine-java:8_jdk",
    dockerRepository := Some("registry.gitlab.com/cosmosim/cosmosim-server"),
    dockerExposedVolumes := Seq(s"/logs"),
    daemonUser := "root",
    dockerUpdateLatest := true,
    javaOptions in Universal ++= Seq(
      "-J-XX:+HeapDumpOnOutOfMemoryError",
      "-J-XX:HeapDumpPath=/logs",
      "-J-Xmx1024m",
      "-J-XX:+UnlockExperimentalVMOptions",
      "-J-XX:+UseCGroupMemoryLimitForHeap")
  )
}
