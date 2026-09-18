/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */
package app.morphe.extension.instagram.patches.userprofile;

import android.util.Log;
import app.morphe.extension.instagram.utils.PikoLog;

public class BioFontTransformer {
    private static final String TAG = "BioFontTransformer";

    public static String transformBio(String rawText, String fontStyle) {
        if (rawText == null || rawText.isEmpty()) {
            return rawText;
        }

        String style = (fontStyle != null) ? fontStyle.toLowerCase().trim() : "";
        String textToTransform = rawText;

        // Fallback: If style is empty/classic/default, check for inline style tags e.g. [editor], [serif], [deco], [signature], [bold]
        if (style.isEmpty() || style.equals("classic") || style.equals("default")) {
            String trimmed = rawText.trim();
            if (trimmed.startsWith("[") && trimmed.contains("]")) {
                int endTag = trimmed.indexOf(']');
                String possibleTag = trimmed.substring(1, endTag).toLowerCase().trim();
                String content = trimmed.substring(endTag + 1).trim();
                if (!content.isEmpty() && isRecognizedStyle(possibleTag)) {
                    style = possibleTag;
                    textToTransform = content;
                    PikoLog.d(TAG, "Detected inline bio font tag: [" + style + "]");
                }
            }
        }

        if (style.isEmpty() || style.equals("classic") || style.equals("default")) {
            return rawText;
        }

        PikoLog.d(TAG, "Transforming bio with style: " + style + ", text: " + textToTransform);

        // 1. Official Instagram Bio Fonts (from LX/0XNI & LX/01QA)
        if (style.equals("editor") || style.contains("editor")) {
            return toMonospace(textToTransform);
        } else if (style.equals("signature") || style.contains("signature")) {
            return toScriptBold(textToTransform);
        } else if (style.equals("serif") || style.contains("serif")) {
            return toSerifBold(textToTransform);
        } else if (style.equals("deco") || style.contains("deco")) {
            return toDoubleStruck(textToTransform);
        }

        // 2. Custom & Extended Styles
        if (style.contains("bold") || style.contains("modern")) {
            return toSansBold(textToTransform);
        } else if (style.contains("italic") || style.contains("slant")) {
            return toSansItalic(textToTransform);
        } else if (style.contains("script") || style.contains("cursive") || style.contains("handwriting")) {
            return toScriptBold(textToTransform);
        } else if (style.contains("typewriter") || style.contains("mono")) {
            return toMonospace(textToTransform);
        } else if (style.contains("gothic") || style.contains("fraktur")) {
            return toFrakturBold(textToTransform);
        } else if (style.contains("outline") || style.contains("double")) {
            return toDoubleStruck(textToTransform);
        } else if (style.contains("small_caps") || style.contains("caps")) {
            return toSmallCaps(textToTransform);
        }

        // Fallback default for unknown non-classic style: Sans Bold
        return toSansBold(textToTransform);
    }

    private static boolean isRecognizedStyle(String tag) {
        if (tag == null || tag.isEmpty()) return false;
        String t = tag.toLowerCase();
        return t.equals("editor") || t.equals("signature") || t.equals("serif") || t.equals("deco") ||
               t.contains("bold") || t.contains("italic") || t.contains("script") || t.contains("mono") ||
               t.contains("gothic") || t.contains("outline") || t.contains("caps");
    }

    public static String sanitizeFontParam(String fontStyle) {
        // Always pass "classic" or empty to the server so server entitlement check passes (HTTP 200 OK)
        PikoLog.d(TAG, "Sanitizing font parameter to 'classic' to bypass server paywall");
        return "classic";
    }

    private static String toSerifBold(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.appendCodePoint(0x1D400 + (c - 'A'));
            } else if (c >= 'a' && c <= 'z') {
                sb.appendCodePoint(0x1D41A + (c - 'a'));
            } else if (c >= '0' && c <= '9') {
                sb.appendCodePoint(0x1D7CE + (c - '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
