package ticketrush;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {}

    public static Object parse(String text) {
        return new Parser(text).parseValue();
    }

    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + escape(s) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(stringify(String.valueOf(entry.getKey()))).append(':').append(stringify(entry.getValue()));
            }
            return sb.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : items) {
                if (!first) sb.append(',');
                first = false;
                sb.append(stringify(item));
            }
            return sb.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported JSON type: " + value.getClass());
    }

    private static String escape(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static final class Parser {
        private final String text;
        private int index = 0;

        private Parser(String text) {
            this.text = text == null ? "" : text.trim();
        }

        private Object parseValue() {
            skip();
            if (index >= text.length()) return null;
            return switch (text.charAt(index)) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++;
            skip();
            if (peek('}')) {
                index++;
                return map;
            }
            while (index < text.length()) {
                String key = parseString();
                skip();
                index++;
                map.put(key, parseValue());
                skip();
                if (peek('}')) {
                    index++;
                    return map;
                }
                index++;
                skip();
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            index++;
            skip();
            if (peek(']')) {
                index++;
                return list;
            }
            while (index < text.length()) {
                list.add(parseValue());
                skip();
                if (peek(']')) {
                    index++;
                    return list;
                }
                index++;
                skip();
            }
            return list;
        }

        private String parseString() {
            StringBuilder sb = new StringBuilder();
            index++;
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') break;
                if (c == '\\' && index < text.length()) {
                    char next = text.charAt(index++);
                    sb.append(switch (next) {
                        case '"', '\\', '/' -> next;
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        default -> next;
                    });
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object parseNumber() {
            int start = index;
            while (index < text.length() && "-0123456789.eE+".indexOf(text.charAt(index)) >= 0) index++;
            String raw = text.substring(start, index);
            if (raw.contains(".") || raw.contains("e") || raw.contains("E")) return Double.parseDouble(raw);
            return Long.parseLong(raw);
        }

        private Object literal(String token, Object value) {
            index += token.length();
            return value;
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void skip() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
        }
    }
}
