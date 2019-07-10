import com.typesafe.sbt.pgp.PgpKeys.publishSigned

val ReleaseTag = """^release/([\d\.]+a?)$""".r

lazy val contributors = Seq(
 "pchlupacek" -> "Pavel Chlupáček"
)


lazy val commonSettings = Seq(
   organization := "com.spinoco",
   scalaVersion := "2.12.8",
   crossScalaVersions := Seq("2.11.12", "2.12.8"),
   scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps",
    "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-value-discard",
    "-Ywarn-unused-import"
   ),
   scalacOptions in (Compile, console) ~= {_.filterNot("-Ywarn-unused-import" == _)},
   scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
   libraryDependencies ++= Seq(
     compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
     , "org.scodec" %% "scodec-bits" % "1.1.12"
     , "org.scodec" %% "scodec-core" % "1.11.4"
     , "com.spinoco" %% "protocol-http" % "0.3.17"
     , "com.spinoco" %% "protocol-websocket" % "0.3.17"
     , "co.fs2" %% "fs2-core" % "1.0.5"
     , "co.fs2" %% "fs2-io" % "1.0.5"
     , "com.spinoco" %% "fs2-crypto" % "0.4.0"
     , "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
   ),
   scmInfo := Some(ScmInfo(url("https://github.com/Spinoco/fs2-http"), "git@github.com:Spinoco/fs2-http.git")),
   homepage := None,
   licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
   initialCommands := s"""
   import fs2._
   import fs2.util.syntax._
   import spinoco.fs2.http
   import http.Resources._
   import spinoco.protocol.http.header._
  """
) ++ testSettings ++ scaladocSettings ++ publishingSettings ++ releaseSettings

lazy val testSettings = Seq(
  parallelExecution in Test := false,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
  publishArtifact in Test := true
)

lazy val scaladocSettings = Seq(
   scalacOptions in (Compile, doc) ++= Seq(
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-implicits",
    "-implicits-show-all"
  ),
   scalacOptions in (Compile, doc) ~= { _ filterNot { _ == "-Xfatal-warnings" } },
   autoAPIMappings := true
)

lazy val publishingSettings = Seq(
  publishTo := {
   val nexus = "https://oss.sonatype.org/"
   if (version.value.trim.endsWith("SNAPSHOT"))
     Some("snapshots" at nexus + "content/repositories/snapshots")
   else
     Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  credentials ++= (for {
   username <- Option(System.getenv().get("SONATYPE_USERNAME"))
   password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq,
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  pomExtra := {
    <url>https://github.com/Spinoco/fs2-http</url>
    <developers>
      {for ((username, name) <- contributors) yield
      <developer>
        <id>{username}</id>
        <name>{name}</name>
        <url>http://github.com/{username}</url>
      </developer>
      }
    </developers>
  },
  pomPostProcess := { node =>
   import scala.xml._
   import scala.xml.transform._
   def stripIf(f: Node => Boolean) = new RewriteRule {
     override def transform(n: Node) =
       if (f(n)) NodeSeq.Empty else n
   }
   val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
   new RuleTransformer(stripTestScope).transform(node)(0)
  }
  , resolvers += Resolver.mavenLocal
)

lazy val releaseSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value
)

lazy val `fs2-http`=
  project.in(file("./"))
  .settings(commonSettings)
  .settings(
    name := "fs2-http"
  )


