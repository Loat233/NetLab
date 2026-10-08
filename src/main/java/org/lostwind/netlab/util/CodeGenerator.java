package org.lostwind.netlab.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class CodeGenerator {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public static String reservationCode() {
        return "RSV-" + getCode();
    }

    public static String borrowCode() {
        return "BRW-" + getCode();
    }

    private static String getCode() {
        String timePart = LocalDateTime.now().format(TIME_FORMATTER);
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);
        return timePart + "-" + randomPart;
    }
}
