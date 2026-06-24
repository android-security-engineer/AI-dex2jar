package com.googlecode.d2j.ai;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free parser for Android binary XML (AXML) — the on-disk
 * format of a compiled {@code AndroidManifest.xml}.
 *
 * <p>This is intentionally a thin event reader: it decodes the string pool and
 * walks the XML chunk stream, emitting START/END element and attribute events.
 * It does not resolve resource references against {@code resources.arsc} — for a
 * manifest that is rarely needed, and string/int/boolean values (the ones that
 * matter for triage: package, sdk levels, permissions, exported flags) decode
 * directly from the manifest itself.
 *
 * <p>Format reference: AOSP {@code ResourceTypes.h}.
 */
public final class AxmlParser {

    // Chunk types.
    private static final int RES_STRING_POOL_TYPE = 0x0001;
    private static final int RES_XML_TYPE = 0x0003;
    private static final int RES_XML_START_NAMESPACE_TYPE = 0x0100;
    private static final int RES_XML_END_NAMESPACE_TYPE = 0x0101;
    private static final int RES_XML_START_ELEMENT_TYPE = 0x0102;
    private static final int RES_XML_END_ELEMENT_TYPE = 0x0103;
    private static final int RES_XML_CDATA_TYPE = 0x0104;
    private static final int RES_XML_RESOURCE_MAP_TYPE = 0x0180;

    // String pool flags.
    private static final int UTF8_FLAG = 0x00000100;

    // Res_value data types.
    private static final int TYPE_REFERENCE = 0x01;
    private static final int TYPE_STRING = 0x03;
    private static final int TYPE_INT_DEC = 0x10;
    private static final int TYPE_INT_HEX = 0x11;
    private static final int TYPE_INT_BOOLEAN = 0x12;

    /** A decoded attribute on a start element. */
    public static final class Attribute {
        public final String namespace;
        public final String name;
        public final String value;

        Attribute(String namespace, String name, String value) {
            this.namespace = namespace;
            this.name = name;
            this.value = value;
        }
    }

    /** Callback for the streaming walk. */
    public interface Handler {
        void startElement(String name, List<Attribute> attrs);

        void endElement(String name);
    }

    private final ByteBuffer buf;
    private String[] stringPool;
    private int[] resourceMap = new int[0];

    public AxmlParser(byte[] data) {
        this.buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }

    public void parse(Handler handler) {
        int fileType = buf.getShort(0) & 0xFFFF;
        if (fileType != RES_XML_TYPE) {
            throw new IllegalArgumentException(
                    "Not a binary XML file (chunk type 0x" + Integer.toHexString(fileType) + ")");
        }
        // Skip the file header (headerSize at offset 2).
        int headerSize = buf.getShort(2) & 0xFFFF;
        int pos = headerSize;

        while (pos + 8 <= buf.limit()) {
            buf.position(pos);
            int type = buf.getShort() & 0xFFFF;
            int chunkHeaderSize = buf.getShort() & 0xFFFF;
            long chunkSize = buf.getInt() & 0xFFFFFFFFL;
            if (chunkSize < 8) {
                break;
            }

            switch (type) {
                case RES_STRING_POOL_TYPE:
                    parseStringPool(pos);
                    break;
                case RES_XML_RESOURCE_MAP_TYPE:
                    parseResourceMap(pos, (int) chunkSize, chunkHeaderSize);
                    break;
                case RES_XML_START_ELEMENT_TYPE:
                    parseStartElement(pos, chunkHeaderSize, handler);
                    break;
                case RES_XML_END_ELEMENT_TYPE:
                    parseEndElement(pos, chunkHeaderSize, handler);
                    break;
                case RES_XML_START_NAMESPACE_TYPE:
                case RES_XML_END_NAMESPACE_TYPE:
                case RES_XML_CDATA_TYPE:
                default:
                    break; // ignored
            }
            pos += (int) chunkSize;
        }
    }

    private void parseResourceMap(int chunkStart, int chunkSize, int headerSize) {
        int count = (chunkSize - headerSize) / 4;
        resourceMap = new int[count];
        buf.position(chunkStart + headerSize);
        for (int i = 0; i < count; i++) {
            resourceMap[i] = buf.getInt();
        }
    }

    private void parseStringPool(int chunkStart) {
        buf.position(chunkStart + 8); // skip ResChunk_header
        int stringCount = buf.getInt();
        buf.getInt(); // styleCount
        int flags = buf.getInt();
        int stringsStart = buf.getInt();
        buf.getInt(); // stylesStart
        boolean utf8 = (flags & UTF8_FLAG) != 0;

        int[] offsets = new int[stringCount];
        for (int i = 0; i < stringCount; i++) {
            offsets[i] = buf.getInt();
        }

        stringPool = new String[stringCount];
        int dataBase = chunkStart + stringsStart;
        for (int i = 0; i < stringCount; i++) {
            stringPool[i] = readPoolString(dataBase + offsets[i], utf8);
        }
    }

    private String readPoolString(int offset, boolean utf8) {
        buf.position(offset);
        if (utf8) {
            skipUtf8Len(); // char count (unused)
            int byteLen = readUtf8Len();
            byte[] bytes = new byte[byteLen];
            buf.get(bytes);
            try {
                return new String(bytes, "UTF-8");
            } catch (Exception e) {
                return new String(bytes);
            }
        } else {
            int charLen = readUtf16Len();
            char[] chars = new char[charLen];
            for (int i = 0; i < charLen; i++) {
                chars[i] = buf.getChar();
            }
            return new String(chars);
        }
    }

    private void skipUtf8Len() {
        int len = buf.get() & 0xFF;
        if ((len & 0x80) != 0) {
            buf.get();
        }
    }

    private int readUtf8Len() {
        int len = buf.get() & 0xFF;
        if ((len & 0x80) != 0) {
            len = ((len & 0x7F) << 8) | (buf.get() & 0xFF);
        }
        return len;
    }

    private int readUtf16Len() {
        int len = buf.getChar();
        if ((len & 0x8000) != 0) {
            len = ((len & 0x7FFF) << 16) | buf.getChar();
        }
        return len;
    }

    private void parseStartElement(int chunkStart, int headerSize, Handler handler) {
        // After ResChunk_header(8) + lineNumber(4) + comment(4) == headerSize (16).
        buf.position(chunkStart + headerSize);
        buf.getInt(); // ns
        int nameRef = buf.getInt();
        buf.getShort(); // attributeStart (== 20)
        buf.getShort(); // attributeSize (== 20)
        int attributeCount = buf.getShort() & 0xFFFF;
        buf.getShort(); // idIndex
        buf.getShort(); // classIndex
        buf.getShort(); // styleIndex

        List<Attribute> attrs = new ArrayList<>(attributeCount);
        for (int i = 0; i < attributeCount; i++) {
            int nsRef = buf.getInt();
            int attrNameRef = buf.getInt();
            int rawValueRef = buf.getInt();
            buf.getShort(); // value size
            buf.get(); // res0
            int dataType = buf.get() & 0xFF;
            int data = buf.getInt();

            String ns = poolStr(nsRef);
            String name = resolveAttrName(attrNameRef);
            String value = decodeValue(dataType, data, rawValueRef);
            attrs.add(new Attribute(ns, name, value));
        }
        handler.startElement(poolStr(nameRef), attrs);
    }

    private void parseEndElement(int chunkStart, int headerSize, Handler handler) {
        buf.position(chunkStart + headerSize);
        buf.getInt(); // ns
        int nameRef = buf.getInt();
        handler.endElement(poolStr(nameRef));
    }

    private String resolveAttrName(int nameRef) {
        String name = poolStr(nameRef);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        // Empty name string: fall back to the resource-map id (common for android:* attrs).
        if (nameRef >= 0 && nameRef < resourceMap.length) {
            String known = AndroidAttrs.name(resourceMap[nameRef]);
            if (known != null) {
                return known;
            }
            return "0x" + Integer.toHexString(resourceMap[nameRef]);
        }
        return name == null ? "" : name;
    }

    private String decodeValue(int dataType, int data, int rawValueRef) {
        switch (dataType) {
            case TYPE_STRING:
                String s = poolStr(data);
                return s != null ? s : poolStr(rawValueRef);
            case TYPE_INT_BOOLEAN:
                return data != 0 ? "true" : "false";
            case TYPE_REFERENCE:
                return "@0x" + Integer.toHexString(data);
            case TYPE_INT_HEX:
                return "0x" + Integer.toHexString(data);
            case TYPE_INT_DEC:
                return Integer.toString(data);
            default:
                if (rawValueRef != -1) {
                    String raw = poolStr(rawValueRef);
                    if (raw != null) {
                        return raw;
                    }
                }
                return Integer.toString(data);
        }
    }

    private String poolStr(int ref) {
        if (stringPool == null || ref < 0 || ref >= stringPool.length) {
            return null;
        }
        return stringPool[ref];
    }
}
