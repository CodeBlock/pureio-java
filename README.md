# pureio-java

[![Build Status](https://travis-ci.org/CodeBlock/pureio-java.svg?branch=master)](https://travis-ci.org/CodeBlock/pureio-java)

Experiments with purely functional IO in Java 8.

This is standalone and doesn't depend on FJ right now, though it probably
should. As such, it re-implements several classes that are commonplace in FJ.

This is experimental, you probably don't want to use it right now.

That being said:

License: **BSD-2**.

## Example

```java
public class Hello {
  private static PureIO<Unit> program =
    TerminalLib.putStrLn("What is your name?")
    .flatMap(unused0 -> TerminalLib.readLine()
             .flatMap(name -> TerminalLib.putStrLn("Hi there, " + name + "! How are you?")
                      .flatMap(unused1 -> TerminalLib.readLine()
                               .flatMap(resp -> TerminalLib.putStrLn("I am also " + resp + "!")
                                        .flatMap(unused2 -> TerminalLib.exit(0))))));

  public static void main(String[] args) {
      UnsafePerformIO.unsafePerformIO(program);
  }
}
```

See more examples in `pureio-examples/`. You might even be able to run them by
doing `sbt pureio-examples/run`, if luck is going your way.

## Building it (Assuming a typical Linux environment)

You need [sbt](https://raw.githubusercontent.com/paulp/sbt-extras/master/sbt)
0.13+ and Java 8. Just download the script, `chmod +x sbt` and place it
somewhere in your `$PATH`.

Once you have sbt installed, you should be able to do this:

```
PATH=/usr/lib/jvm/java-1.8.0-openjdk.x86_64/bin/:$PATH sbt -java-home /usr/lib/jvm/java-1.8.0-openjdk.x86_64/ pureio-examples/run
```

For convenience, you can add this to your bash_profile for future use:

```
alias sbt8='PATH=/usr/lib/jvm/java-1.8.0-openjdk.x86_64/bin/:$PATH sbt -java-home /usr/lib/jvm/java-1.8.0-openjdk.x86_64/'
```

and then use `sbt8 pureio-examples/run` to run the examples.

## Building it (Windows-ish)

Windows Cygwin/MinGW support for building is experimental. We apparently can't
use paulp's sbt script because it requires Bash 4 while MinGW defaults to Bash
3. That said, you should be able to follow the instructions on the
[sbt website](http://www.scala-sbt.org/0.13/tutorial/Manual-Installation.html#Windows)
with minimal difficulty, after installing Java 8.

Once you have a working `sbt` command, you can proceed as normal, and use
`sbt pureio-examples/run` to run the examples.


## JavaDoc

[View javadoc](https://codeblock.github.io/pureio-java/).
