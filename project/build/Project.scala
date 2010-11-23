import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {

  val minaCore = "org.apache.mina" % "mina-core" % "2.0.1"
  val slf4j = "org.slf4j" % "slf4j-api" % "1.6.1"
  val slf4j_jdk14 = "org.slf4j" % "slf4j-jdk14" % "1.6.1"

  override val mainClass = Some("castle.gui.LocalServerClientMain")

}
