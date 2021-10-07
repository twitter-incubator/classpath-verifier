# classpath-verifier

`classpath-verifier` is a classpath verifier for JVM applications. Given a classpath and a set of
entry points, it verifies that all reachable code links and will not fail at runtime with a linkage
error.

 * [How to use](#how-to-use)
   * [Using as a standalone application](#using-as-a-standalone-application)
   * [Using as a library](#using-as-a-library)
 * [Support](#support)
 * [Contributing](#contributing)
 * [Security issues?](#security-issues)

## How to use

`classpath-verifier` can be used as a standalone application, or used as a library.

### Using as a standalone application

**classpath-verifier is not published anywhere at the moment. Please refer to 
[CONTRIBUTING.md](CONTRIBUTING.md) for information on how to build classpath-verifier locally.**

The main class is called `com.twitter.classpathverifier.Main`.
```
classpath-verifier 0.1
Usage: classpath-verifier [options]

  --classpath <value>      The full classpath of the application
  -e, --entry <value>      The entrypoints to link from
  -j, --jar <value>        JARs to fully check
  --discover-mains <value>
                           JARs whose main methods to discover and check
  --entrypoint-from-manifest <value>
                           JARs whose manifest to read to discover the entrypoints
  -p, --path <value>       Whether to show the path from entrypoint to missing symbol
  -h, --javahome <value>   The path to the javahome to use while linking (defaults to <...>)
  --dot-output <value>     The path where to write the DOT dependency graph
  --dot-granularity <value>
                           The DOT graph granularity. Possible values: package, class, method
  --dot-package-filter <value>
                           Package name to include in the DOT graph
```

`--classpath` is used to define the classpath of the application. It accepts a list of
classpath entries separated by `:`. This option can be specified more than once.

`--entry` is used to define one entrypoint of the application. The entrypoints of the
application are the methods from which the linker will discover all reachable symbols and emit
errors when a reachable symbol cannot be found. The entrypoints can be specified using a
dedicated syntax described in [Entrypoint syntax](#entrypoint-syntax). This option can be
specified more than once.

`--jar` is used to specify that all of the methods of all the classes contained in the jar
should be considered as entrypoints. This option can be specified more than once.

`--discover-mains`  is used to specify that all of the `main` methods of the given jar file
should be considered as entrypoints. This option can be specified more than once.

`--entrypoint-from-manifest` is used to specify that the given jar's manifest should be read,
and the mainclass specified there should be added as an entrypoint. This option can be
specified more than once.

`--path` configures whether the path from an entrypoint to a missing symbol should be
reconstructed, when a missing symbol is encountered.

`--javahome` configures the javahome the linker should use to read classes from the JDK. It
defaults to the current JVM's javahome.

`--dot-output` configures the path where a dot graph representing the application's
reachable symbols will be written.

`--dot-granularity` configures the level of details that should be included in the dot graph.
Possible values are `package` (show only packages and their dependencies), `class` (show only classes and their dependencies) or `method` (show methods and their dependencies).

`--dot-package-filter` is used to restrict the dot graph to only the specified packages
and their subpackages. Symbols belonging to other packages will not be included in the graph.
This option can be specified more than once.

#### Entrypoint syntax
The entrypoints can be specified using the following syntax:

```
<fully qualified class name>#<method name>:<method descriptor>
```

Where the `<method descriptor>` is a method descriptor according to [§4.3.3 of the JVM
Specification](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3).

When targetting a method called `main` and whose descriptor is `([java.lang.String;)V`, then the
method name and method descriptor can be omitted:
```
# These 2 are equivalent:
com.acme.MyClass
com.acme.MyClass#main:([java.lang.String;)V
```

When targetting a method whose descriptor is `([java.lang.String;)V`, then the method
descriptor can be omitted:
```
# These 2 are equivalent:
com.acme.MyClass#myMethod
com.acme.MyClass#myMethod:([java.lang.String;)V
```

### Using as a library

For the simplest cases, where you simply want to verify a given classpath against a set of
entrypoints, you can directly call

```scala
Linker.verify(
  classpath: List[Path],
  entrypoints: List[Reference.Method]
): Seq[LinkerError]
```

Here's a complete example:

```scala
import com.twitter.classpathverifier.Reference
import com.twitter.classpathverifier.linker.Linker

// The classpath to verify.
val classpath: List[Path] = List(Paths.get("my-app.jar"))

// The application's entrypoints, from which reachable
// methods will be discovered.
val entrypoints: List[Reference.Method] = List(
  // short form: equivalent to com.acme.Main#main:([Ljava.lang.String;)V
  // will match `def main(args: Array[String]): Unit` in `object com.acme.Main`
  Reference.Method("com.acme.Main"),

  // equivalent to com.acme.Clazz#myMethod:([Ljava.lang.String;)V
  // will match `def myMethod(args: Array[String]): Unit`
  Reference.Method("com.acme.Clazz#myMethod"), // signature is assumed to be 

  // full syntax
  // will match `def someMeth(a: Boolean, b: Boolean): Int`
  Reference.Method("com.acme.Foo#someMeth:(ZZ)I")
)

// Discover missing symbols
val errors = Linker.verify(classpath, entrypoints)
```

If you need more configuration, then you can create a `Config` object, initialize a
`Context` and pass that to the linker:

```scala
import com.twitter.classpathverifier.config.Config
import com.twitter.classpathverifier.linker.Context
import com.twitter.classpathverifier.linker.Linker

// Create a configuration
val config = Config.empty
  .copy(
    classpath = ...,
    entrypoints = ...
  )

// Initialize the context
val context = Context.init(config)

// Discover missing symbols
val errors = Linker.verify(context)
```

## Support

Create a [new issue](https://github.com/twitter-incubator/classpath-verifier/issues/new) on GitHub.

## Contributing

Contributing information can be found in [CONTRIBUTING.md](CONTRIBUTING.md).

We feel that a welcoming community is important and we ask that you follow Twitter's
[Open Source Code of Conduct](https://github.com/twitter/code-of-conduct/blob/master/code-of-conduct.md)
in all interactions with the community.


## Security Issues?

Please report sensitive security issues via Twitter's bug-bounty program
(https://hackerone.com/twitter) rather than GitHub.
