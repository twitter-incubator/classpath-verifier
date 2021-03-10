val scala213 = "2.13.6"
val scala212 = "2.12.14"
val scala3 = "3.0.0"

inThisBuild(
  Seq(
    licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
    organization := "com.twitter",
    homepage := Some(url("https://github.com/twitter-incubator/classpath-verifier")),
    developers := List(
      Developer(
        "@Duhemm",
        "Martin Duhem",
        "martin.duhem@gmail.com",
        url("https://github.com/Duhemm")
      ),
      Developer(
        "@cattibrie",
        "Ekaterina Tyurina",
        "etyurina@twitter.com",
        url("https://github.com/cattibrie")
      )
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/twitter-incubator/classpath-verifier"),
        "scm:git:git@github.com:twitter-incubator/classpath-verifier.git"
      )
    ),
    publish / skip := true,
    scalaVersion := scala213,
    crossScalaVersions := Seq(scala212, scala213, scala3),
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.2",
    scalafixScalaBinaryVersion := "2.13",
    scalafixCaching := true,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions ++= List(
      "-deprecation",
      "-Ywarn-unused:imports",
      "-Yrangepos"
    )
  )
)

lazy val `classpath-verifier` = project
  .in(file("classpath-verifier"))
  .enablePlugins(BuildInfoPlugin, NativeImagePlugin)
  .settings(
    publish / skip := false,
    libraryDependencies += Dependencies.scopt,
    libraryDependencies += Dependencies.asm,
    libraryDependencies += Dependencies.collectionCompat,
    libraryDependencies += Dependencies.coursier % Test,
    libraryDependencies += Dependencies.munit % Test,
    libraryDependencies += Dependencies.scalaCompiler.value % Test,
    testFrameworks += new TestFramework("munit.Framework"),
    Test / buildInfoKeys := Seq[BuildInfoKey](
      target,
      scalaVersion,
      scalaBinaryVersion
    ),
    Test / buildInfoPackage := "com.twitter.classpathverifier",
    BuildInfoPlugin.buildInfoScopedSettings(Test),
    BuildInfoPlugin.buildInfoDefaultSettings
  )
