package com.esports.domain.stat;

import java.text.Normalizer;
import java.util.Locale;

final class StatNameNormalizer {

    private StatNameNormalizer() {
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9가-힣]", "");
    }
}
