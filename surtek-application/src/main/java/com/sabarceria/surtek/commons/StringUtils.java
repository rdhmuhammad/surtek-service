package com.sabarceria.surtek.commons;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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
}
