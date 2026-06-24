---
description: Read an APK's v1 signing certificate — subject, validity, and fingerprints
allowed-tools: Bash(python3:*), Bash(chmod:*)
argument-hint: <file.apk>
---

# apk-cert - APK Signing Certificate Inspector

Read the v1 (JAR) signing certificate from an APK and report its identity and fingerprints.

## Usage

```bash
python3 d2j-ai.py apk-cert $ARGUMENTS
```

## What it does

Extracts the PKCS#7 signing block from `META-INF/*.RSA|*.DSA|*.EC` and decodes the
embedded X.509 certificate. Reports:

- **subject** / **issuer** distinguished names (and whether the cert is self-signed)
- **serial** number and **validity** window (`not_before` / `not_after`)
- **signature algorithm** and **public key** type/size
- **SHA-256**, **SHA-1**, and **MD5** fingerprints of the certificate

The certificate fingerprint is the strongest *same-author* signal across a corpus: apps
signed with the same key share an identical fingerprint regardless of package name or
version. Use it to cluster samples and attribute related apps.

> Note: only v1 (JAR-signature) certificates are decoded. v2/v3 APK Signing Block
> signers are not parsed, but for author attribution the v1 cert (when present) almost
> always mirrors the v2/v3 signer.

## Options

- `-o, --output <path>` — write output to file instead of stdout

## Output

JSON with `v1_signed`, `signature_files[]`, and `certificates[]`, where each entry is
`{from, subject, issuer, self_signed, serial, not_before, not_after, sig_algorithm,
public_key, sha256, sha1, md5}`.

## Example Workflows

### Identify the signer of an APK
```bash
python3 d2j-ai.py apk-cert app.apk
```

### Cluster two APKs by signer (compare sha256)
```bash
python3 d2j-ai.py apk-cert a.apk | jq -r '.certificates[].sha256'
python3 d2j-ai.py apk-cert b.apk | jq -r '.certificates[].sha256'
```

## Prerequisites

- Java 8+ (uses the JDK's `CertificateFactory`)
- Build: `./gradlew :dex2jar-ai-cli:build`
