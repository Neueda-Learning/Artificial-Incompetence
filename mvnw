#!/bin/sh

# Maven Wrapper startup script

MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.9-bin"

case "$(uname)" in
  CYGWIN*|MINGW*|MSYS*)
    # Use the .cmd wrapper on Windows Git Bash
    exec cmd.exe /c "$(cd "$(dirname "$0")" && pwd)/mvnw.cmd" "$@"
    ;;
  *)
    # Unix-like
    JAVA_HOME="${JAVA_HOME:-}"
    if [ -n "$JAVA_HOME" ]; then
      JAVACMD="$JAVA_HOME/bin/java"
    else
      JAVACMD="java"
    fi

    WRAPPER_JAR="$(cd "$(dirname "$0")" && pwd)/.mvn/wrapper/maven-wrapper.jar"

    if [ ! -f "$WRAPPER_JAR" ]; then
      echo "Maven Wrapper JAR not found at $WRAPPER_JAR" >&2
      exit 1
    fi

    exec "$JAVACMD" -jar "$WRAPPER_JAR" "$@"
    ;;
esac
