---
name: sensitive-api-audit
description: "Hunt sensitive or dangerous API usage inside a DEX/APK and trace who reaches it — cryptography, reflection, dynamic code loading, and network calls — then walk the call graph back to the entry points. Use when the user asks where the crypto/encryption is, whether the app loads code at runtime, where it talks to the network, whether it uses reflection to hide behavior, who calls a specific method/class, or to find a specific symbol's usage sites. Triggers include: find the crypto, where does it encrypt, does it load code dynamically, find network calls, does it use reflection, where is this API used, who calls this method, find usages of, trace callers, 找加密的地方, 有没有动态加载, 哪里发网络请求, 用了反射吗, 谁调用了这个方法, 查一下这个api在哪用的. Use proactively when reviewing an app for hidden or sensitive behavior."
license: Apache-2.0
---

# sensitive-api-audit — Reverse Xref & Caller Tracing

Find *where* sensitive behavior lives in a DEX/APK and *how it's reached*, by
combining reverse cross-reference with call-graph tracing. Runs through
`python3 d2j-ai.py … --pretty`; reads the DEX directly (no decompile needed).

## 1. Locate the sinks — `dex-xref`

Use a preset for the common sensitive-API families, or `--to` for a specific
symbol:

```bash
python3 d2j-ai.py dex-xref app.apk --preset crypto --pretty      # javax.crypto, Cipher, SecretKey…
python3 d2j-ai.py dex-xref app.apk --preset reflection --pretty  # Class.forName, getMethod, invoke…
python3 d2j-ai.py dex-xref app.apk --preset dynload --pretty     # DexClassLoader, loadLibrary…
python3 d2j-ai.py dex-xref app.apk --preset net --pretty         # URL, HttpURLConnection, OkHttp, Socket…
python3 d2j-ai.py dex-xref app.apk --to "doFinal" --pretty       # any substring: method/field/type/string
```

Each match reports the **kind** (method/field/type/string), the matched
**symbol**, the **enclosing method** (the usage site), and the **opcode**. Use
`-k method,field` to restrict the kinds. This turns "see structure" into "find the
sink."

## 2. Trace back to entry points — `dex-method-trace`

For a sink worth understanding, find who calls it:

```bash
python3 d2j-ai.py dex-method-trace app.apk --target "Cipher.doFinal" --pretty
```

`--target` reports all callers of methods matching the substring (owner.name).
Repeat up the chain to reach an exported component, `onCreate`, a JNI bridge, or a
message handler — that's the reachable entry point.

## 3. Corroborate

- `dex-strings -f` for the literals near a sink (key material, algorithm names,
  URLs) — e.g. `dex-strings app.apk -f AES`.
- `dex-class-deps -c <class> -d` to see what a suspicious class pulls in.
- If the trail enters native code, switch to the `apk-triage` / `dex2jar` skills'
  `native-libs` step and read the `Java_*` bridges.

## Output

Report each finding as: **family** (crypto/reflection/dynload/net), **sink**
(symbol + site), **reachability** (the caller chain to an entry point), and
**corroborating evidence** (strings/deps). Call out anything that combines
dynamic loading + network + reflection — a classic hidden-payload pattern.

This is authorized security review of an app the user already holds.
