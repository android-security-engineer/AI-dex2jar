package com.googlecode.d2j.ai;

import com.googlecode.dex2jar.tools.BaseCmd;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Read the v1 (JAR / APK Signature Scheme v1) signing certificates from an APK and
 * report subject/issuer/serial/validity plus SHA-256 / SHA-1 / MD5 fingerprints.
 *
 * <p>Certificate fingerprints are the strongest "same author" signal across a corpus:
 * apps signed by the same private key share an identical certificate fingerprint even
 * when package names, versions, and code differ. The v1 certificate lives in
 * {@code META-INF/*.RSA|*.DSA|*.EC} as a PKCS#7 block, which the JDK's
 * {@link CertificateFactory} decodes directly — no external dependency.
 *
 * <p>v2/v3 signing blocks (in the APK Signing Block) are not parsed here; for author
 * attribution the v1 certificate, when present, is sufficient and almost always mirrors
 * the v2/v3 signer.
 */
public class ApkCertCmd extends BaseCmd {

    @Opt(opt = "o", longOpt = "output", description = "Output file path (default: stdout)")
    String output;

    @Override
    protected void doCommandLine() throws Exception {
        if (remainingArgs == null || remainingArgs.length == 0) {
            throw new HelpException("No input file specified");
        }
        Path input = Paths.get(remainingArgs[0]);
        if (!Files.exists(input)) {
            System.err.println("File not found: " + input);
            return;
        }

        List<String> certObjs = new ArrayList<>();
        List<String> signatureFiles = new ArrayList<>();
        boolean v1Present = false;

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        try (ZipFile zf = new ZipFile(input.toFile())) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            List<ZipEntry> sigBlocks = new ArrayList<>();
            for (ZipEntry e = null; entries.hasMoreElements(); ) {
                e = entries.nextElement();
                String n = e.getName();
                String upper = n.toUpperCase();
                if (upper.startsWith("META-INF/")
                        && (upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC"))) {
                    sigBlocks.add(e);
                    signatureFiles.add(n);
                }
            }
            v1Present = !sigBlocks.isEmpty();

            for (ZipEntry e : sigBlocks) {
                try (InputStream in = zf.getInputStream(e)) {
                    Collection<? extends java.security.cert.Certificate> certs = cf.generateCertificates(in);
                    for (java.security.cert.Certificate c : certs) {
                        if (c instanceof X509Certificate) {
                            certObjs.add(certToJson(e.getName(), (X509Certificate) c));
                        }
                    }
                }
            }
        }

        Json.Arr sigArr = Json.arr();
        for (String s : signatureFiles) {
            sigArr.add(s);
        }
        Json.Arr certArr = Json.arr();
        for (String c : certObjs) {
            certArr.addRaw(c);
        }

        String result = Json.obj()
                .put("v1_signed", v1Present)
                .put("signature_block_count", signatureFiles.size())
                .putRaw("signature_files", sigArr.build())
                .put("certificate_count", certObjs.size())
                .putRaw("certificates", certArr.build())
                .build();

        emit(result);
    }

    private static String certToJson(String fromEntry, X509Certificate c) throws Exception {
        byte[] der = c.getEncoded();
        Json.Obj o = Json.obj()
                .put("from", fromEntry)
                .put("subject", str(c.getSubjectX500Principal().getName()))
                .put("issuer", str(c.getIssuerX500Principal().getName()))
                .put("self_signed", c.getSubjectX500Principal().equals(c.getIssuerX500Principal()))
                .put("serial", c.getSerialNumber().toString(16))
                .put("not_before", str(c.getNotBefore().toInstant().toString()))
                .put("not_after", str(c.getNotAfter().toInstant().toString()))
                .put("sig_algorithm", str(c.getSigAlgName()))
                .put("public_key", publicKeyDesc(c.getPublicKey()))
                .put("sha256", hex(digest("SHA-256", der)))
                .put("sha1", hex(digest("SHA-1", der)))
                .put("md5", hex(digest("MD5", der)));
        return o.build();
    }

    private static String publicKeyDesc(PublicKey key) {
        if (key instanceof RSAPublicKey) {
            return "RSA " + ((RSAPublicKey) key).getModulus().bitLength() + " bit";
        }
        if (key instanceof ECPublicKey) {
            int size = ((ECPublicKey) key).getParams().getCurve().getField().getFieldSize();
            return "EC " + size + " bit";
        }
        return key.getAlgorithm();
    }

    private static String str(String s) {
        return s == null ? "" : s;
    }

    private static byte[] digest(String algo, byte[] data) throws Exception {
        return MessageDigest.getInstance(algo).digest(data);
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }

    private void emit(String result) throws Exception {
        if (output != null && !output.isEmpty()) {
            Files.write(Paths.get(output), result.getBytes("UTF-8"));
            System.out.println("Written to " + output);
        } else {
            System.out.println(result);
        }
    }
}
