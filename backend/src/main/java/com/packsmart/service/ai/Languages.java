package com.packsmart.service.ai;

/** Explanation languages supported by the AI layer. */
final class Languages {

    private Languages() {
    }

    static String normalise(String lang) {
        return lang == null ? "en" : switch (lang) {
            case "hi", "mr" -> lang;
            default -> "en";
        };
    }

    static String name(String lang) {
        return switch (normalise(lang)) {
            case "hi" -> "Hindi (Devanagari script, simple everyday words)";
            case "mr" -> "Marathi (Devanagari script, simple everyday words)";
            default -> "simple English";
        };
    }
}
