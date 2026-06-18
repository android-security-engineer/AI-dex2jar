---
description: Switch Java .class file version in JAR files
allowed-tools: Bash(python3:*)
argument-hint: <version> <old.jar> <new.jar>
---

# class-version-switch - Switch Class File Version

Change the Java .class file version in a JAR file.

## Usage

```bash
python3 d2j-ai.py class-version-switch $ARGUMENTS
```

## What it does

Modifies the class file version number in all .class files within a JAR. This can make JARs compiled with newer JDK versions loadable on older JVMs (at the bytecode level, without guaranteed API compatibility).

## Version Numbers

Common Java version numbers:
- 52 = Java 8
- 51 = Java 7
- 50 = Java 6
- 49 = Java 5

## Output

Returns JSON with:
- `success` — true/false
- `stdout` — version switch output log
- `stderr` — any errors or warnings

## Example

```bash
python3 d2j-ai.py class-version-switch 50 app.jar app_java6.jar
```
