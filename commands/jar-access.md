---
description: Modify access flags in JAR files - make classes/methods/fields public for analysis
allowed-tools: Bash(python3:*)
argument-hint: <jar-file> [-ac public] [-am public] [-af public]
---

# jar-access - Modify JAR Access Flags

Add or remove access modifiers on classes, methods, and fields in JAR files. Useful for making private members accessible during reverse engineering.

## Usage

```bash
python3 d2j-ai.py jar-access $ARGUMENTS
```

## Options

- `-ac, --add-class-access <flag>` — Add access to classes (e.g. public)
- `-am, --add-method-access <flag>` — Add access to methods
- `-af, --add-field-access <flag>` — Add access to fields
- `-rc, --remove-class-access <flag>` — Remove access from classes
- `-rm, --remove-method-access <flag>` — Remove access from methods
- `-rf, --remove-field-access <flag>` — Remove access from fields

## Example

Make all classes and methods public:
```bash
python3 d2j-ai.py jar-access app.jar -ac public -am public
```
