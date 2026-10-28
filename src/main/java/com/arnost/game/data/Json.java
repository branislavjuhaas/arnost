package com.arnost.game.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small, dependency-free JSON reader for the game's bundled data files.
 *
 * <p>The project intentionally keeps the runtime jar free of third-party
 * libraries. The data files are authored in the repository, so this parser
 * only needs to support the standard JSON value types and strict syntax.</p>
 */
public final class Json {
    private Json() {
    }

    public static Map<String, Object> readObject(String resource) {
        try (InputStream input = Json.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing JSON resource: " + resource);
            }
            String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return object(parse(text), resource);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read JSON resource: " + resource, e);
        }
    }

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.value();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw parser.error("Unexpected trailing content");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value, String context) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(context + ": expected a JSON object");
        }
        return (Map<String, Object>) value;
    }

    public static Map<String, Object> object(Map<String, Object> parent, String key) {
        return object(required(parent, key, key), key);
    }

    public static String string(Map<String, Object> parent, String key) {
        return string(required(parent, key, key), key);
    }

    public static String optionalString(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        return value == null ? null : string(value, key);
    }

    public static String string(Object value, String context) {
        if (!(value instanceof String result)) {
            throw new IllegalArgumentException(context + ": expected a string");
        }
        return result;
    }

    public static int integer(Map<String, Object> parent, String key) {
        return integer(required(parent, key, key), key);
    }

    public static int integer(Object value, String context) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(context + ": expected an integer");
        }
        double decimal = number.doubleValue();
        if (decimal != Math.rint(decimal)
            || decimal < Integer.MIN_VALUE || decimal > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(context + ": expected an integer");
        }
        return (int) decimal;
    }

    public static double decimal(Map<String, Object> parent, String key) {
        return decimal(required(parent, key, key), key);
    }

    public static double decimal(Object value, String context) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(context + ": expected a number");
        }
        return number.doubleValue();
    }

    public static List<String> stringList(Object value, String context) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(context + ": expected an array");
        }
        List<String> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            result.add(string(list.get(i), context + "[" + i + "]"));
        }
        return result;
    }

    public static List<Map<String, Object>> objectList(Object value, String context) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(context + ": expected an array");
        }
        List<Map<String, Object>> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            result.add(object(list.get(i), context + "[" + i + "]"));
        }
        return result;
    }

    public static Map<String, String> stringMap(Object value, String context) {
        Map<String, Object> raw = object(value, context);
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(), string(entry.getValue(),
                context + "." + entry.getKey()));
        }
        return result;
    }

    public static Map<String, Double> doubleMap(Object value, String context) {
        Map<String, Object> raw = object(value, context);
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(), decimal(entry.getValue(),
                context + "." + entry.getKey()));
        }
        return result;
    }

    public static Map<String, Integer> integerMap(Object value, String context) {
        Map<String, Object> raw = object(value, context);
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(), integer(entry.getValue(),
                context + "." + entry.getKey()));
        }
        return result;
    }

    public static Map<String, List<String>> stringListMap(Object value, String context) {
        Map<String, Object> raw = object(value, context);
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(), List.copyOf(stringList(entry.getValue(),
                context + "." + entry.getKey())));
        }
        return result;
    }

    private static Object required(Map<String, Object> parent, String key, String context) {
        if (!parent.containsKey(key)) {
            throw new IllegalArgumentException(context + ": missing required key '" + key + "'");
        }
        return parent.get(key);
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text;
        }

        private Object value() {
            skipWhitespace();
            if (atEnd()) {
                throw error("Expected a value");
            }
            return switch (text.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> {
                    char c = text.charAt(index);
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        yield number();
                    }
                    throw error("Unexpected character '" + c + "'");
                }
            };
        }

        private Map<String, Object> object() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = string();
                skipWhitespace();
                expect(':');
                Object value = value();
                result.put(key, value);
                skipWhitespace();
                if (consume('}')) {
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> array() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return result;
            }
            while (true) {
                result.add(value());
                skipWhitespace();
                if (consume(']')) {
                    return result;
                }
                expect(',');
            }
        }

        private String string() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (!atEnd()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return result.toString();
                }
                if (c == '\\') {
                    if (atEnd()) {
                        throw error("Unterminated escape sequence");
                    }
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case '"' -> result.append('"');
                        case '\\' -> result.append('\\');
                        case '/' -> result.append('/');
                        case 'b' -> result.append('\b');
                        case 'f' -> result.append('\f');
                        case 'n' -> result.append('\n');
                        case 'r' -> result.append('\r');
                        case 't' -> result.append('\t');
                        case 'u' -> result.append(unicodeEscape());
                        default -> throw error("Invalid escape sequence \\" + escaped);
                    }
                } else if (c < 0x20) {
                    throw error("Control character in string");
                } else {
                    result.append(c);
                }
            }
            throw error("Unterminated string");
        }

        private char unicodeEscape() {
            if (index + 4 > text.length()) {
                throw error("Incomplete unicode escape");
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                throw error("Invalid unicode escape \\u" + hex);
            }
        }

        private Number number() {
            int start = index;
            if (peek() == '-') {
                index++;
            }
            if (atEnd()) {
                throw error("Incomplete number");
            }
            char first = text.charAt(index);
            if (first == '0') {
                index++;
            } else if (first >= '1' && first <= '9') {
                index++;
                while (isDigit(peek())) {
                    index++;
                }
            } else {
                throw error("Invalid number");
            }

            boolean decimal = false;
            if (peek() == '.') {
                decimal = true;
                index++;
                if (!isDigit(peek())) {
                    throw error("Expected a digit after the decimal point");
                }
                while (isDigit(peek())) {
                    index++;
                }
            }
            if (peek() == 'e' || peek() == 'E') {
                decimal = true;
                index++;
                if (peek() == '+' || peek() == '-') {
                    index++;
                }
                if (!isDigit(peek())) {
                    throw error("Expected an exponent digit");
                }
                while (isDigit(peek())) {
                    index++;
                }
            }

            String token = text.substring(start, index);
            try {
                return decimal ? Double.valueOf(token) : Long.valueOf(token);
            } catch (NumberFormatException e) {
                throw error("Invalid number '" + token + "'");
            }
        }

        private Object literal(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw error("Expected '" + literal + "'");
            }
            index += literal.length();
            return value;
        }

        private void expect(char expected) {
            if (atEnd() || text.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean consume(char expected) {
            if (!atEnd() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private char peek() {
            return atEnd() ? '\0' : text.charAt(index);
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private void skipWhitespace() {
            while (!atEnd()) {
                char c = text.charAt(index);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    index++;
                } else {
                    return;
                }
            }
        }

        private boolean atEnd() {
            return index >= text.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + index);
        }
    }
}
