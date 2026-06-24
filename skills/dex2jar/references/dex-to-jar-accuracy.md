# DEX → JAR: how it works and when it's wrong

## What the conversion actually does

`dex2jar` translates **Dalvik bytecode** (register-based, in the `.dex`) into
**JVM bytecode** (stack-based, in `.class` files inside a `.jar`). It is a
bytecode-to-bytecode translation, *not* decompilation:

1. Read the DEX (classes, methods, instructions) via the dex-reader.
2. Build an intermediate representation (dex-ir) of each method body.
3. Map register-based Dalvik ops to stack-based JVM ops, reconstructing the
   operand stack, local variable table, and exception tables.
4. Emit `.class` files and zip them into a `.jar`.

The resulting JAR is what a Java decompiler (JD-GUI, CFR, Procyon, Fernflower)
then turns into readable `.java`. So "decompile an APK" is really two steps:
`dex2jar` (this tool) → a decompiler (separate).

## Why it is not always accurate

The Dalvik and JVM type/stack models are not identical, so translation can be
lossy or fail outright. Expect trouble with:

- **Obfuscated / hand-crafted DEX** — tools like obfuscators or packers emit
  bytecode that is valid for ART but violates assumptions the stack
  reconstruction makes (e.g. irreducible control flow, ambiguous register types).
- **Type confusion** — a Dalvik register can hold an int or a reference at
  different points; the JVM verifier is stricter, so the recovered types may not
  verify.
- **Synthetic / version-specific constructs** — newer DEX features or unusual
  compilers can produce patterns the translator handles imperfectly.
- **Anti-decompilation tricks** — deliberately crafted to break exactly this step.

When translation can't produce verifiable bytecode for a method, dex2jar may emit
a stub, throw inside that method, or drop fidelity — the class loads but the body
is wrong or incomplete.

## How to detect and respond

1. **Verify the output:**
   ```bash
   python3 d2j-ai.py dex2jar app.apk --pretty
   python3 d2j-ai.py asm-verify app-dex2jar.jar --pretty
   ```
   `asm-verify` runs the ASM verifier over the produced `.class` files and reports
   methods that don't verify — those are the ones to distrust.

2. **Cross-check against ground truth** when a method looks wrong: disassemble the
   *original* DEX with `baksmali` and read the smali. Smali reflects the real
   Dalvik bytecode and never goes through the lossy DEX→JVM mapping, so it's the
   authoritative view when the JAR is suspect.

3. **For analysis (not editing)**, prefer the structured commands (`dex-inspect`,
   `dex-strings`, `dex-xref`, `dex-method-trace`) — they read the DEX directly and
   sidestep the conversion entirely.

## Rule of thumb

Use the JAR for *readability*, smali for *ground truth*, and always `asm-verify`
before trusting a converted JAR for anything load-bearing. Tell the user when a
method failed verification rather than presenting decompiled output as fact.
