import ReleaseTransformations._

def majorVersion(s: String): String =
  s.substring(0, s.lastIndexOf('.'))

def javaAgent(v: String, m: String, prefix: String): String =
  s"-javaagent:${prefix}target/scala-${m}/clouseau_${m}-${v}.jar"

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false)

lazy val clouseauSettings = Seq(
  organization := "org.spire-math",
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.10.6", "2.11.11", "2.12.4"),
  libraryDependencies ++=
    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test ::
    Nil,
  scalacOptions ++=
    "-deprecation" ::
    "-encoding" ::
    "UTF-8" ::
    "-feature" ::
    "-language:existentials" ::
    "-language:higherKinds" ::
    "-language:implicitConversions" ::
    "-language:experimental.macros" ::
    "-unchecked" ::
    "-Xfatal-warnings" ::
    "-Xlint" ::
    "-Yno-adapted-args" ::
    "-Ywarn-dead-code" ::
    "-Ywarn-numeric-widen" ::
    "-Ywarn-value-discard" ::
    "-Xfuture" ::
    Nil,
  // HACK: without these lines, the console is basically unusable,
  // since all imports are reported as being unused (and then become
  // fatal errors).
  scalacOptions in (Compile, console) ~= { _.filterNot("-Xlint" == _) },
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,

  // make sure to fork for testing/repl
  fork := true,

  // settings for packaging instrumentation correctly
  packageOptions in (Compile, packageBin) +=
    Package.ManifestAttributes("Premain-Class" -> "clouseau.Inst"),

  // release stuff
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    //runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges
  ),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("Snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("Releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <url>https://github.com/non/clouseau</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
        <comments>A business-friendly OSS license</comments>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:non/clouseau.git</url>
      <connection>scm:git:git@github.com:non/clouseau.git</connection>
    </scm>
    <developers>
      <developer>
        <id>non</id>
        <name>Erik Osheim</name>
        <url>http://github.com/non/</url>
      </developer>
    </developers>
  )
)

lazy val root = project
  .in(file("."))
  .settings(clouseauSettings: _*)
  .settings(noPublish: _*)
  .settings(Seq(
    name := "clouseau-root",
    moduleName := "clouseau-root"))
  .dependsOn(core, repl)
  .aggregate(core, repl)

lazy val core = project
  .in(file("core"))
  .settings(clouseauSettings: _*)
  .settings(Seq(
    name := "clouseau",
    moduleName := "clouseau",
    // we need to use the java agent when running tests
    javaOptions += javaAgent(version.value, majorVersion(scalaVersion.value), ""),
    // we need to package the jar before running the tests
    (test in Test) := {
      (Keys.`package` in Compile).value
      (test in Test).value
    }))

lazy val repl = project
  .in(file("repl"))
  .settings(clouseauSettings: _*)
  .settings(Seq(
    name := "clouseau-repl",
    moduleName := "clouseau-repl",
    libraryDependencies ++=
      "org.scala-lang" % "scala-compiler" % scalaVersion.value ::
      Nil,
    mainClass in Compile := Some("clouseau.Repl"),
    // we need some settings to hook up the repl's stdin/stdout
    outputStrategy := Some(StdoutOutput),
    connectInput in run := true,
    // we need to use the java agent when running the repl
    javaOptions += javaAgent(version.value, majorVersion(scalaVersion.value), "../core/")))
  .dependsOn(core)

lazy val bench = project
  .in(file("bench"))
  .settings(clouseauSettings: _*)
  .settings(noPublish: _*)
  .settings(Seq(
    name := "clouseau-bench",
    moduleName := "clouseau-bench",
    // libraryDependencies ++=
    //   "org.scala-lang" % "scala-compiler" % scalaVersion.value ::
    //   Nil,
    // mainClass in Compile := Some("clouseau.Repl"),
    // we need some settings to hook up the repl's stdin/stdout
    // outputStrategy := Some(StdoutOutput),
    // connectInput in run := true,
    // we need to use the java agent when running the repl
    javaOptions += javaAgent(version.value, majorVersion(scalaVersion.value), "../core/")))
  .dependsOn(core)
  .enablePlugins(JmhPlugin)
