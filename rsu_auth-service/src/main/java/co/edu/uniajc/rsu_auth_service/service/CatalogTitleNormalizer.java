package co.edu.uniajc.rsu_auth_service.service;

import java.util.Locale;

public final class CatalogTitleNormalizer {
    private static final Locale SPANISH_COLOMBIA = Locale.forLanguageTag("es-CO");

    private CatalogTitleNormalizer() {
    }

    public static String normalize(String value) {
        String lowerCase = value.trim().toLowerCase(SPANISH_COLOMBIA);
        StringBuilder titleCase = new StringBuilder(lowerCase.length());
        boolean capitalizeNextLetter = true;

        for (int offset = 0; offset < lowerCase.length();) {
            int codePoint = lowerCase.codePointAt(offset);
            if (Character.isLetter(codePoint)) {
                titleCase.appendCodePoint(capitalizeNextLetter ? Character.toUpperCase(codePoint) : codePoint);
                capitalizeNextLetter = false;
            } else {
                titleCase.appendCodePoint(codePoint);
                int type = Character.getType(codePoint);
                boolean combiningMark = type == Character.NON_SPACING_MARK
                        || type == Character.COMBINING_SPACING_MARK
                        || type == Character.ENCLOSING_MARK;
                if (Character.isWhitespace(codePoint) || (!combiningMark && !Character.isLetterOrDigit(codePoint))) {
                    capitalizeNextLetter = true;
                }
            }
            offset += Character.charCount(codePoint);
        }

        return titleCase.toString();
    }
}
