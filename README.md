# nowarn plugin

This is a Scala 2 and 3 compiler plugin to configure custom annotations that expand into `@annotation.nowarn`
annotations before the typer phase.

## Install

Add the resolver and compiler plugin dependency to your `build.sbt`:

```scala
resolvers += "bondlink-maven-repo" at "https://raw.githubusercontent.com/mblink/maven-repo/main"
addCompilerPlugin("bondlink" %% "nowarn-plugin" % "1.1.1")
```

## Configure

To configure a new annotation via the plugin, pass additional flags to `scalac`. The format of a flag is:

```
-P:nowarn:<annotation-name>:<nowarn-config>
```

Where `nowarn-config` is a string to be passed to `@annotation.nowarn`, e.g. `cat=deprecation` or `msg=never used`. You can find more info on [configuring the `nowarn` annotation here](https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html#configuring-warnings).

For example, to create an `@unused` annotation that expands to `@annotation.nowarn("msg=never used")` add the following to your `build.sbt`:

```scala
scalacOptions += "-P:nowarn:unused:msg=never used"
```

You can also put periods in the annotation name to mirror your preferred package path, e.g.

```scala
scalacOptions += "-P:nowarn:my.pkg.unused:msg=never used"
```

## Use

Once you've configured an annotation via a `scalac` option, you can use it in your code just like any other annotation:

```scala
def hasUnusedParam(@unused x: Int) = ...
def hasUnusedParam(@my.pkg.unused x: Int) = ...
```
