package com.ddvader44.officebuddy;

import java.io.File;
import java.util.ArrayList;

public final class FileScanner {

    public interface Filter {
        boolean accept(File f);
    }

    private FileScanner() {}

    /** Recursively collect files under dir matching filter, sorted by last-modified (newest first). */
    public static ArrayList<File> scan(File dir, Filter filter) {
        ArrayList<File> out = new ArrayList<>();
        scanRecursive(dir, filter, out, 0);
        out.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return out;
    }

    private static void scanRecursive(File dir, Filter filter, ArrayList<File> out, int depth) {
        if (dir == null || depth > 12) return;               // depth guard against symlink loops
        File[] entries = dir.listFiles();
        if (entries == null) return;
        for (File f : entries) {
            if (f.isDirectory()) {
                scanRecursive(f, filter, out, depth + 1);
            } else if (filter.accept(f)) {
                out.add(f);                                   // File objects are unique; no name-based dedup needed
            }
        }
    }

    public static boolean hasExtension(File f, String... exts) {
        String name = f.getName().toLowerCase();
        for (String e : exts) {
            if (name.endsWith(e.toLowerCase())) return true;  // case-insensitive
        }
        return false;
    }
}
