## *iota*  Compiler

*iota* is a compiler for a language called *iota*. Refer to [The *iota* Language Specification](langspec) 
for the formal details about the language. The compiler targets a register-based machine called 
[Harvey Mudd Miniature Machine (HMMM)](https://www.cs.hmc.edu/~cs5grad/cs5/hmmm/documentation/documentation.html).

The following command compiles the compiler:
```bash
$ ant
```

The following command runs the compiler and prints the usage string:
```bash
$ ./bin/iota
```

The following command compiles a test *iota* program `Factorial.iota` using the *iota* compiler, which translates 
the program into an HMMM program called `Factorial.hmmm`:
```bash
$ ./bin/iota tests/Factorial.iota
```

The following command assembles and simulates the `HelloWorld.hmmm` program:
```bash
$ python3 ./bin/hmmm.py Factorial.hmmm
```

## Software Dependencies

* [OpenJDK](https://openjdk.org/)
* [Ant](https://ant.apache.org/)
* [Python](https://www.python.org/)
