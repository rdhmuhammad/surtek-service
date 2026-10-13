package com.sabarceria.surtek.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtils {

    private static Pattern snakeCasePattern = Pattern.compile("[a-z]+(_[a-z]+)*");

    private static Pattern cemalCasePattern = Pattern.compile("^[a-zA-Z]+([A-Z][a-z]+)+$");

    public static String toSnakeCase(String str) {
        str = str.trim();
        if (cemalCasePattern.matcher(str).matches()) {
            List<String> words = Arrays.stream(str.split("(?=[A-Z])")).toList();
            words = words.stream().map(String::toLowerCase).toList();
            return String.join("_", words);
        }

        return str;
    }

    public static String toCapitalizeWord(String str) {
        str = str.trim();
        if (snakeCasePattern.matcher(str).matches()) {
            return capitalizeWord(str, "_");
        } else if (cemalCasePattern.matcher(str).matches()) {
            return capitalizeWord(str, "(?=[A-Z])");
        }
        return str;
    }

    private static String capitalizeWord(String word, String delimiter) {
        List<String> words = Arrays.stream(word.split(delimiter)).toList();
        words = words.stream().map(org.springframework.util.StringUtils::capitalize).toList();

        return String.join(" ", words);
    }

    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom random = new SecureRandom();

    public static Boolean IsNullOrEmpty(String key) {
        return Optional.ofNullable(key).orElse("").isEmpty();
    }

    public static <T> Boolean IsNullOrEmpty(List<T> keys) {
        return Optional.ofNullable(keys).orElse(new ArrayList<>()).isEmpty();
    }

    public static String toString(Object object) {
        String result = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }


    public static String generateCode(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 2; i++) {
            sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }
        // Add 3 digits
        for (int i = 0; i < 3; i++) {
            sb.append(random.nextInt(10));
        }
        // Add 1 lowercase letter
        sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        // Add dash and 1 digit
//        sb.append('-').append(random.nextInt(10));
        return sb.toString();
    }

    public static String generateCodeV2(String prefix) {
        int targetLength = 10; // final string length
        String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random random = new Random();

        StringBuilder sb = new StringBuilder(prefix);

        // Ensure the dash is always there after the prefix
        if (!prefix.endsWith("-")) {
            sb.append("-");
        }

        // Fill until total = 10
        while (sb.length() < targetLength) {
            if (random.nextBoolean()) {
                sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
            } else {
                sb.append(random.nextInt(10));
            }
        }

        // Trim if longer than 10
        if (sb.length() > targetLength) {
            sb.setLength(targetLength);
        }

        return sb.toString();
    }

    public static boolean isValidUrl(String url) {
        String regex = "^(https?://)([\\w.-]+)(:[0-9]{1,5})?(/.*)?$";
        return url != null && url.matches(regex);
    }

    public static String toTitleCase(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        return Arrays.stream(text.split("_"))
                .filter(word -> !word.isBlank())
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );

    public static Optional<String> extractEmail(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = EMAIL_PATTERN.matcher(input);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }

        return Optional.empty();
    }

}
