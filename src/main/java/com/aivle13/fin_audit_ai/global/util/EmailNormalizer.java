package com.aivle13.fin_audit_ai.global.util;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}