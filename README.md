# WaveShooter
Space shooter with unique audio interaction.

## Building & running
This project is written with a modern JDK (24+), but the default `java` on some
machines is older (Java 8). To avoid the unsupported class version error, either
install a matching runtime **or** compile for Java 8 compatibility as shown below.

On Windows you can simply run the provided batch file:

```bat
build.bat
```

It will compile the sources with `--release 8` and then launch `game.Main`.

Alternatively, compile manually:

```sh
javac --release 8 -d bin game\*.java  # outputs to bin/
java -cp bin game.Main
```

## Timeline goal:
10/1 - Black hole entities to interact with. Stars near black hole get sucked in and dissappear near edge.
10/8 - Space map implemented. Find other blackholes to zap. Switch to .wav samples. Map variables to sound dynaimcs.
10/15 - Level implementation
11/1 - Polish and version 1.0 complete
