package com.optical.common.util;

import org.springframework.util.StringUtils;

public final class StringNormalizer {

    private StringNormalizer() {
    }

    public static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
