package com.sabarceria.surtek.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValueOf {
    private static final Logger log = LoggerFactory.getLogger(ValueOf.class);

    private ValueOf() {
        throw new IllegalStateException("ValueOf is utility class");
    }

    private static boolean isNull(Object value) {
        if (Objects.isNull(value)) {
            return true;
        } else if (value instanceof String) {
            String valueStr = (String)value;
            return valueStr.trim().isEmpty();
        } else {
            return false;
        }
    }

    public static boolean isNull(Object... values) {
        for(Object o : values) {
            if (isNull(o)) {
                return true;
            }
        }

        return false;
    }

    public static boolean inverse(boolean value) {
        return !value;
    }

    public static String init(String value, String defaultValue) {
        return isNull((Object)value) ? defaultValue : value;
    }

    public static Integer init(Integer value, Integer defaultValue) {
        return isNull((Object)value) ? defaultValue : value;
    }

    public static Float init(Float value, Float defaultValue) {
        return isNull((Object)value) ? defaultValue : value;
    }

    public static Long init(Long value, Long defaultValue) {
        return isNull((Object)value) ? defaultValue : value;
    }

    public static Double init(Double value, Double defaultValue) {
        return isNull((Object)value) ? defaultValue : value;
    }

    public static Boolean init(Boolean value, Boolean defaultValue) {
        return isNull((Object)value) ? defaultValue : value;
    }

    public static boolean nonNull(Object value) {
        return !isNull(value);
    }

    public static boolean nonNull(String value) {
        return !isNull((Object)value);
    }

    public static boolean nonNull(Double value) {
        return !isNull((Object)value);
    }

    public static boolean nonNull(Boolean value) {
        return !isNull((Object)value);
    }

    public static boolean equalText(String string, String... equals) {
        if (isNull((Object)string)) {
            return false;
        } else {
            for(String s : equals) {
                if (!isNull((Object)s) && string.equalsIgnoreCase(s)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean notEqualText(String string, String... equals) {
        if (isNull((Object)string)) {
            return true;
        } else {
            for(String s : equals) {
                if (!isNull((Object)s) && string.equalsIgnoreCase(s)) {
                    return false;
                }
            }

            return true;
        }
    }

    public static boolean containsText(String string, String... equals) {
        if (isNull((Object)string)) {
            return false;
        } else {
            for(String s : equals) {
                if (!isNull((Object)s) && string.contains(s)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean containsTextIgnoreCase(String string, String... equals) {
        if (isNull((Object)string)) {
            return false;
        } else {
            for(String s : equals) {
                if (!isNull((Object)s) && string.toUpperCase().contains(s.toUpperCase())) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean notContainsTextIgnoreCase(String string, String... equals) {
        if (isNull((Object)string)) {
            return true;
        } else {
            for(String s : equals) {
                if (!isNull((Object)s) && string.toUpperCase().contains(s.toUpperCase())) {
                    return false;
                }
            }

            return true;
        }
    }

    public static boolean startWithText(String string, String... prefix) {
        if (isNull((Object)string)) {
            return false;
        } else {
            for(String s : prefix) {
                if (!isNull((Object)s) && string.startsWith(s)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean equalInt(Integer value, Integer... values) {
        if (isNull((Object)value)) {
            return false;
        } else {
            for(Integer s : values) {
                if (!isNull((Object)s) && value == s) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean minMaxLength(int min, int max, String value) {
        if (Objects.isNull(value)) {
            return false;
        } else {
            int length = value.trim().length();
            return minMaxLength(min, max, length);
        }
    }

    public static boolean minMaxLength(int min, int max, int length) {
        return length >= min && length <= max;
    }

    public static boolean notLength(String value, int length) {
        return !lengthEquals(value, length);
    }

    public static boolean lengthEquals(String value, int length) {
        if (Objects.isNull(value)) {
            return true;
        } else {
            return value.length() == length;
        }
    }

    public static boolean isNumber(String value) {
        if (Objects.isNull(value)) {
            return false;
        } else {
            boolean result = isValidRegex("^\\d+$", value);
            log.debug("is number {}, {}", value, result);
            return result;
        }
    }

    public static boolean isNotNumber(String value) {
        return !isNumber(value);
    }

    public static boolean isValidRegex(String regex, String value) {
        if (Objects.isNull(value)) {
            return false;
        } else {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(value);
            return m.find();
        }
    }

    public static Optional<String> ofNullable(String value) {
        return Optional.ofNullable(init((String)value, (String)null));
    }

    public static Optional<Double> ofNullable(Double value) {
        return Optional.ofNullable(init((Double)value, (Double)null));
    }

    public static Optional<Long> ofNullable(Long value) {
        return Optional.ofNullable(init((Long)value, (Long)null));
    }

    public static Optional<Float> ofNullable(Float value) {
        return Optional.ofNullable(init((Float)value, (Float)null));
    }

    public static Optional<Integer> ofNullable(Integer value) {
        return Optional.ofNullable(init((Integer)value, (Integer)null));
    }

    public static Optional<Boolean> ofNullable(Boolean value) {
        return Optional.ofNullable(init((Boolean)value, (Boolean)null));
    }

    public static boolean isDouble(String income) {
        try {
            Double.valueOf(income);
            return true;
        } catch (Exception var2) {
            return false;
        }
    }
}
