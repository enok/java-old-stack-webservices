package com.campusconnect.sis.common.util;

/**
 * Hand-rolled helpers that predate the commons-lang dependency and were never
 * removed. commons-lang 2.6 is also on the classpath and does all of this.
 *
 * TODO: delete this class and use org.apache.commons.lang.StringUtils.
 */
public final class LegacyStringUtils {

    private LegacyStringUtils() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() == 0 ? null : t;
    }

    /**
     * Minimal XML escaping used by the string-concatenation response builder in
     * com.campusconnect.sis.transform.ResponseShaper.
     *
     * XXX: this is NOT a complete XML escaper. It does not handle control
     * characters or surrogate pairs. It is the only thing standing between a
     * student's apostrophe and a malformed SOAP response. See SMELL #2.
     */
    public static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else if (c == '\'') {
                sb.append("&apos;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
