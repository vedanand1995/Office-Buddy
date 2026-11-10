package com.ddvader44.officebuddy.render;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps font names found inside documents to bundled or user-imported typefaces,
 * with a fallback chain and a user-selectable default font.
 *
 * Bundled fonts are resolved by resource name at runtime (getIdentifier), so the
 * app still compiles if some font resources have not been added yet:
 *   res/font/roboto_regular.ttf, roboto_bold.ttf, roboto_italic.ttf
 *   res/font/times_new_roman.ttf   (use Tinos, metric-compatible)
 *   res/font/courier_prime.ttf
 *   res/font/noto_sans.ttf, noto_sans_devanagari.ttf, noto_sans_gujarati.ttf
 *   res/font/krushnadev.ttf, lmg_arun.ttf   (legacy Gujarati fonts)
 */
public final class FontResolver {

    private static final Map<String, Typeface> CACHE = new HashMap<>();

    private FontResolver() {}

    /** User-chosen default font, persisted. */
    public static Typeface userDefault(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String name = prefs.getString("default_font", "roboto");
        return forName(ctx, name);
    }

    public static void setUserDefault(Context ctx, String fontKey) {
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().putString("default_font", fontKey).apply();
    }

    /** Map a document font name (e.g. "Calibri", "Times New Roman", "LMG Arun") to a typeface. */
    public static Typeface forDocumentFont(Context ctx, String docFontName, boolean bold, boolean italic) {
        if (docFontName != null && !docFontName.trim().isEmpty()) {
            // 1) An exactly-named user-imported font always wins
            Typeface exact = FontImporter.imported(ctx, docFontName);
            if (exact != null) return typeface(exact, bold, italic);

            String n = docFontName.toLowerCase();
            if (n.contains("krushnadev")) {
                return forName(ctx, "krushnadev", bold, italic);
            }
            if (n.contains("lmg") || n.contains("arun") || n.contains("nilkanth")
                    || n.contains("harikikani") || n.contains("gopika") || n.contains("avantika")) {
                return forName(ctx, "lmg_arun", bold, italic);
            }
            if (n.contains("gujarati") || n.contains("shruti") || n.contains("lohit")) {
                return forName(ctx, "noto_sans_gujarati", bold, italic);
            }
            if (n.contains("devanagari") || n.contains("mangal") || n.contains("kokila")) {
                return forName(ctx, "noto_sans_devanagari", bold, italic);
            }
            if (n.contains("times") || n.contains("georgia") || n.contains("garamond")) {
                return forName(ctx, "times", bold, italic);
            }
            if (n.contains("courier") || n.contains("mono") || n.contains("consolas")) {
                return forName(ctx, "courier", bold, italic);
            }
        }
        // Unknown font -> user's chosen default
        return userDefault(ctx);
    }

    public static Typeface forName(Context ctx, String key) {
        return forName(ctx, key, false, false);
    }

    public static Typeface forName(Context ctx, String key, boolean bold, boolean italic) {
        int style = (bold ? Typeface.BOLD : 0) | (italic ? Typeface.ITALIC : 0);
        // 1) User-imported fonts (by exact name) win over bundled ones
        Typeface imported = FontImporter.imported(ctx, key);
        if (imported != null) return typeface(imported, bold, italic);
        // 2) Bundled fonts, resolved by resource name so missing ttf files don't break the build
        String resName = bundledResName(key);
        if (resName != null) {
            int resId = ctx.getResources().getIdentifier(resName, "font", ctx.getPackageName());
            if (resId != 0) {
                String cacheKey = resName + "|" + style;
                Typeface cached = CACHE.get(cacheKey);
                if (cached != null) return cached;
                Typeface t = ctx.getResources().getFont(resId);
                t = Typeface.create(t, style);
                CACHE.put(cacheKey, t);
                return t;
            }
        }
        // 3) System fallback
        return Typeface.create(Typeface.SANS_SERIF, style);
    }

    private static String bundledResName(String key) {
        switch (key) {
            case "times":               return "times_new_roman";
            case "courier":             return "courier_prime";
            case "noto":                return "noto_sans";
            case "devanagari":
            case "noto_sans_devanagari":return "noto_sans_devanagari";
            case "noto_sans_gujarati":  return "noto_sans_gujarati";
            case "krushnadev":          return "krushnadev";
            case "lmg_arun":            return "lmg_arun";
            case "roboto":              return "roboto_regular";
            default:                    return null;
        }
    }

    private static Typeface typeface(Typeface base, boolean bold, boolean italic) {
        int style = (bold ? Typeface.BOLD : 0) | (italic ? Typeface.ITALIC : 0);
        return Typeface.create(base, style);
    }
}
