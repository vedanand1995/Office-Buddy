package com.ddvader44.officebuddy.render;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Lets users import their own .ttf fonts, either one file at a time or a whole
 * .zip archive of fonts. Fonts are stored app-privately, no permission needed.
 */
public final class FontImporter {

    private FontImporter() {}

    /** Directory where imported fonts are stored (app-private, no permission needed). */
    public static File fontsDir(Context ctx) {
        File dir = new File(ctx.getExternalFilesDir(null), "fonts");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** All user-imported fonts. */
    public static List<File> importedFonts(Context ctx) {
        List<File> out = new ArrayList<>();
        File[] files = fontsDir(ctx).listFiles((d, name) -> name.toLowerCase().endsWith(".ttf"));
        if (files != null) for (File f : files) out.add(f);
        return out;
    }

    /** Look up an imported font by display name (filename without extension, case-insensitive). */
    public static Typeface imported(Context ctx, String displayName) {
        if (displayName == null) return null;
        for (File f : importedFonts(ctx)) {
            String base = f.getName().substring(0, f.getName().lastIndexOf('.'));
            if (base.equalsIgnoreCase(displayName.trim())) {
                try {
                    return Typeface.createFromFile(f);
                } catch (RuntimeException e) {
                    return null;   // corrupt / unsupported ttf
                }
            }
        }
        return null;
    }

    /**
     * Import ONE file picked via SAF. Accepts a .ttf directly, or a .zip containing .ttf files.
     * Returns the display names of fonts successfully imported.
     */
    public static List<String> importFromUri(Context ctx, Uri uri) throws IOException {
        String name = queryDisplayName(ctx, uri);
        if (name != null && name.toLowerCase().endsWith(".zip")) {
            return importZip(ctx, uri);
        }
        File dest = new File(fontsDir(ctx), sanitize(name != null ? name : "imported.ttf"));
        copy(ctx, uri, dest);
        List<String> result = new ArrayList<>();
        result.add(stripExt(dest.getName()));
        return result;
    }

    /** Import every .ttf inside a .zip (flattens folders, skips other file types). */
    public static List<String> importZip(Context ctx, Uri zipUri) throws IOException {
        List<String> imported = new ArrayList<>();
        InputStream in = ctx.getContentResolver().openInputStream(zipUri);
        if (in == null) throw new IOException("Cannot open file");
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = new File(entry.getName()).getName();   // flatten folders
                if (entry.isDirectory() || !entryName.toLowerCase().endsWith(".ttf")) continue;
                File dest = new File(fontsDir(ctx), sanitize(entryName));
                OutputStream os = new FileOutputStream(dest);
                try {
                    int len;
                    while ((len = zis.read(buf)) > 0) os.write(buf, 0, len);
                } finally {
                    os.close();
                }
                imported.add(stripExt(dest.getName()));
                zis.closeEntry();
            }
        }
        return imported;
    }

    /** Delete an imported font by display name. */
    public static boolean deleteFont(Context ctx, String displayName) {
        for (File f : importedFonts(ctx)) {
            String base = f.getName().substring(0, f.getName().lastIndexOf('.'));
            if (base.equalsIgnoreCase(displayName)) return f.delete();
        }
        return false;
    }

    private static void copy(Context ctx, Uri uri, File dest) throws IOException {
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("Cannot open file");
        OutputStream os = new FileOutputStream(dest);
        try {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
        } finally {
            try { is.close(); } catch (IOException ignored) {}
            os.close();
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._ -]", "_");
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    private static String queryDisplayName(Context ctx, Uri uri) {
        android.database.Cursor c = ctx.getContentResolver().query(uri, null, null, null, null);
        if (c == null) return null;
        try {
            int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (c.moveToFirst() && idx >= 0) return c.getString(idx);
        } finally {
            c.close();
        }
        return null;
    }
}
