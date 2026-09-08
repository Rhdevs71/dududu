/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */
package app.morphe.extension.instagram.patches.userprofile;

import android.util.Log;

public class BioFontTransformer {
    private static final String TAG = "BioFontTransformer";

    public static String transformBio(String rawText, String fontStyle) {
        if (rawText == null || rawText.isEmpty()) {
            return rawText;
        }
        if (fontStyle == null || fontStyle.isEmpty() || fontStyle.equalsIgnoreCase("classic") || fontStyle.equalsIgnoreCase("default")) {
            return rawText;
        }

        Log.d(TAG, "Transforming bio with style: " + fontStyle + ", length: " + rawText.length());
        String style = fontStyle.toLowerCase();

        if (style.contains("bold") || style.contains("modern")) {
            return toSansBold(rawText);
        } else if (style.contains("italic") || style.contains("slant")) {
            return toSansItalic(rawText);
        } else if (style.contains("script") || style.contains("cursive") || style.contains("handwriting")) {
            return toScriptBold(rawText);
        } else if (style.contains("typewriter") || style.contains("mono")) {
            return toMonospace(rawText);
        } else if (style.contains("gothic") || style.contains("fraktur")) {
            return toFrakturBold(rawText);
        } else if (style.contains("outline") || style.contains("double")) {
            return toDoubleStruck(rawText);
        } else if (style.contains("small_caps") || style.contains("caps")) {
            return toSmallCaps(rawText);
        }

        // Fallback default for unknown non-classic style: Sans Bold
        return toSansBold(rawText);
    }

    public static String sanitizeFontParam(String fontStyle) {
        // Always pass "classic" or empty to the server so server entitlement check passes (HTTP 200 OK)
        Log.d(TAG, "Sanitizing font parameter to 'classic' to bypass server paywall");
        return "classic";
    }

    private static String toSansBold(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.appendCodePoint(0x1D5D4 + (c - 'A'));
            } else if (c >= 'a' && c <= 'z') {
                sb.appendCodePoint(0x1D5EE + (c - 'a'));
            } else if (c >= '0' && c <= '9') {
                sb.appendCodePoint(0x1D7EC + (c - '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toSansItalic(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.appendCodePoint(0x1D608 + (c - 'A'));
            } else if (c >= 'a' && c <= 'z') {
                sb.appendCodePoint(0x1D622 + (c - 'a'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toScriptBold(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.appendCodePoint(0x1D4D0 + (c - 'A'));
            } else if (c >= 'a' && c <= 'z') {
                sb.appendCodePoint(0x1D4EA + (c - 'a'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toMonospace(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.appendCodePoint(0x1D670 + (c - 'A'));
            } else if (c >= 'a' && c <= 'z') {
                sb.appendCodePoint(0x1D68A + (c - 'a'));
            } else if (c >= '0' && c <= '9') {
                sb.appendCodePoint(0x1D7F6 + (c - '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toFrakturBold(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.appendCodePoint(0x1D56C + (c - 'A'));
            } else if (c >= 'a' && c <= 'z') {
                sb.appendCodePoint(0x1D586 + (c - 'a'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toDoubleStruck(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 'C') {
                sb.append('\u2102');
            } else if (c == 'H') {
                sb.append('\u210D');
            } else if (c == 'N') {
                sb.append('\u2115');
            } else if (c == 'P') {
                sb.append('\u2119');
            } else if (c == 'Q') {
                sb.append('\u211A');
            } else if (c == 'R') {
                sb.append('\u211D');
            } else if (c == 'Z') {
                sb.append('\u2124');
            } else if (c >= 'A' && c <= 'Z') {
                sb.appendCodePoint(0x1D538 + (c - 'A'));
            } else if (c >= 'a' && c <= 'z') {
                sb.appendCodePoint(0x1D552 + (c - 'a'));
            } else if (c >= '0' && c <= '9') {
                sb.appendCodePoint(0x1D7D8 + (c - '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toSmallCaps(String text) {
        final String normal = "abcdefghijklmnopqrstuvwxyz";
        final String smallCaps = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char lower = Character.toLowerCase(c);
            int idx = normal.indexOf(lower);
            if (idx >= 0) {
                sb.append(smallCaps.charAt(idx));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
