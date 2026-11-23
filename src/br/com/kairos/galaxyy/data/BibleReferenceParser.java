package br.com.kairos.galaxyy.data;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BibleReferenceParser {

    public static class ParsedRef {
        public final String normalizedBook;
        public final int chapter;
        public final int verseStart;
        public final int verseEnd;

        public ParsedRef(String normalizedBook, int chapter, int verseStart, int verseEnd) {
            this.normalizedBook = normalizedBook;
            this.chapter = chapter;
            this.verseStart = verseStart;
            this.verseEnd = verseEnd;
        }
    }

    private static final Pattern REF_PATTERN = Pattern.compile(
            "^((?:[1-3]\\s*)?[^\\d:;]+?)\\s*(\\d+)(?:[:.](\\d+)(?:\\s*(?:[-\\u2013]|,)\\s*(\\d+))?)?"
    );

    private static final Map<String, String> ALIASES = new HashMap<String, String>();

    static {
        ALIASES.put("salmo", "salmos");
        ALIASES.put("atos", "atos dos apostolos");
        ALIASES.put("cantares", "canticos");
        ALIASES.put("cantares de salomao", "canticos");
        ALIASES.put("cantico dos canticos", "canticos");
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String lower = input.trim().toLowerCase();
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            switch (c) {
                case 'á': case 'à': case 'â': case 'ã': case 'ä': sb.append('a'); break;
                case 'é': case 'è': case 'ê': case 'ë': sb.append('e'); break;
                case 'í': case 'ì': case 'î': case 'ï': sb.append('i'); break;
                case 'ó': case 'ò': case 'ô': case 'õ': case 'ö': sb.append('o'); break;
                case 'ú': case 'ù': case 'û': case 'ü': sb.append('u'); break;
                case 'ç': sb.append('c'); break;
                default: sb.append(c); break;
            }
        }
        String collapsed = sb.toString().replaceAll("\\s+", " ").trim();
        if (collapsed.startsWith("iii ")) {
            collapsed = "3 " + collapsed.substring(4);
        } else if (collapsed.startsWith("ii ")) {
            collapsed = "2 " + collapsed.substring(3);
        } else if (collapsed.startsWith("i ")) {
            collapsed = "1 " + collapsed.substring(2);
        }
        if (collapsed.length() > 1 && collapsed.charAt(0) >= '1' && collapsed.charAt(0) <= '3'
                && collapsed.charAt(1) != ' ') {
            collapsed = collapsed.charAt(0) + " " + collapsed.substring(1);
        }
        String alias = ALIASES.get(collapsed);
        return alias != null ? alias : collapsed;
    }

    public static ParsedRef parse(String rawRef) {
        if (rawRef == null) {
            return null;
        }
        String trimmed = rawRef.trim();
        if (trimmed.length() == 0) {
            return null;
        }
        Matcher m = REF_PATTERN.matcher(trimmed);
        if (!m.find()) {
            return null;
        }
        String bookNorm = normalize(m.group(1));
        if (bookNorm.length() == 0) {
            return null;
        }
        try {
            int chapter = Integer.parseInt(m.group(2));
            int vStart = m.group(3) != null ? Integer.parseInt(m.group(3)) : -1;
            int vEnd = m.group(4) != null ? Integer.parseInt(m.group(4)) : vStart;
            if (vEnd < vStart) {
                vEnd = vStart;
            }
            return new ParsedRef(bookNorm, chapter, vStart, vEnd);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
