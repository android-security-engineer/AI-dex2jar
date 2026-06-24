package com.googlecode.d2j.ai;

/**
 * Minimal dependency-free JSON builder shared by the analysis commands.
 *
 * <p>The analysis commands historically each hand-rolled their own {@code jsonStr}
 * escaper and string concatenation. This class centralizes escaping and provides a
 * tiny fluent object/array builder so every command emits identical, well-formed JSON.
 *
 * <p>Output is compact (no pretty-printing); the Python wrapper / MCP layer re-formats
 * when a human-readable view is requested.
 *
 * <pre>{@code
 * String out = Json.obj()
 *     .put("count", 3)
 *     .putRaw("items", Json.arr().add("a").add("b").build())
 *     .build();
 * }</pre>
 */
public final class Json {

    private Json() {
    }

    /** Quote and escape a string value, or return the literal {@code null}. */
    public static String str(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static Obj obj() {
        return new Obj();
    }

    public static Arr arr() {
        return new Arr();
    }

    /** Fluent JSON object builder. */
    public static final class Obj {
        private final StringBuilder sb = new StringBuilder("{");
        private boolean first = true;

        private void key(String k) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(Json.str(k)).append(':');
        }

        /** Add a string-valued field (value is escaped/quoted). */
        public Obj put(String k, String v) {
            key(k);
            sb.append(Json.str(v));
            return this;
        }

        public Obj put(String k, long v) {
            key(k);
            sb.append(v);
            return this;
        }

        public Obj put(String k, boolean v) {
            key(k);
            sb.append(v);
            return this;
        }

        /** Add a field whose value is already-serialized JSON (object, array, number…). */
        public Obj putRaw(String k, String rawJson) {
            key(k);
            sb.append(rawJson);
            return this;
        }

        public String build() {
            return sb.toString() + "}";
        }
    }

    /** Fluent JSON array builder. */
    public static final class Arr {
        private final StringBuilder sb = new StringBuilder("[");
        private boolean first = true;

        private void sep() {
            if (!first) {
                sb.append(',');
            }
            first = false;
        }

        /** Add a string element (escaped/quoted). */
        public Arr add(String v) {
            sep();
            sb.append(Json.str(v));
            return this;
        }

        public Arr add(long v) {
            sep();
            sb.append(v);
            return this;
        }

        /** Add an already-serialized JSON element. */
        public Arr addRaw(String rawJson) {
            sep();
            sb.append(rawJson);
            return this;
        }

        public boolean isEmpty() {
            return first;
        }

        public String build() {
            return sb.toString() + "]";
        }
    }
}
