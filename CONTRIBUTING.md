# How to Contribute

We'd love to get patches from you!

## Building the Project

You'll need to install both git and sbt on your machine to build
classpath-verifier.

In order to build classpath-verifier, open an sbt shell:

```
$ sbt
# compile the project:
> compile
# run locally built:
> classpath-verifier/run
[info] running com.twitter.classpathverifier.Main
Error: Missing option --classpath
classpath-verifier 0.1
(...)
```

Since classpath-verifier is not published anywhere at the moment, the easiest
way to run it outside of sbt is to publish classpath-verifier on your machine,
then run it via [coursier](https://get-coursier.io):

```
$ sbt
> classpath-verifier/version
... # copy this result
> publishLocal
> exit
$ cs launch com.twitter:classpath-verifier_2.13:<paste version>
```

## Workflow

We follow the [GitHub Flow
Workflow](https://guides.github.com/introduction/flow/), which typically
involves forking the project into your GitHub account, adding your changes to a
feature branch, and opening a Pull Request to contribute those changes back to
the project.

## Testing

To run the tests, open an sbt shell

```
$ sbt
# run all the tests:
> test
# run a single test suite:
> testOnly com.twitter.classpathverifier.linker.LinkerSuite
# run a single test case:
> testOnly com.twitter.classpathverifier.linker.LinkerSuite -- "*test name*"
# run all the tests with all Scala versions:
> + test
```

## Style

Code style is enforced with scalafmt and scalafix. To format your sources, run
`scalafmtAll` and `scalafixAll` in sbt:

```
$ sbt
> scalafmtAll
[info] Formatting 8 Scala sources...
[info] Formatting 7 Scala sources...
> scalafixAll
[info] compiling 1 Scala source to /home/you/classpath-verifier/classpath-verifier/target/scala-2.13/classes ...
[info] Running scalafix on 1 Scala sources (incremental)
```

## Issues

When filing an issue, try to adhere to the provided template if applicable.  In
general, the more information you can provide about exactly how to reproduce a
problem, the easier it will be to help fix it.

# Code of Conduct

We expect all contributors to abide by our [Code of
Conduct](https://github.com/twitter/.github/blob/master/code-of-conduct.md).
