package org.lineageos.settings.device;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

/**
 * Pure file I/O utility for reading and writing device nodes.
 */
public final class FileUtils {
    private static final String TAG = "DeviceParts-FileUtils";

    private FileUtils() {}

    public static boolean fileExists(String path) {
        return path != null && new File(path).exists();
    }

    public static boolean writeLine(String path, String value) {
        if (path == null || value == null) return false;
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(value.getBytes());
            fos.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to write to " + path + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean writeLine(String path, int value) {
        return writeLine(path, String.valueOf(value));
    }

    public static String readOneLine(String path) {
        if (path == null) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
            return br.readLine();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read from " + path + ": " + e.getMessage());
            return null;
        }
    }

    public static int readInt(String path, int defaultValue) {
        String line = readOneLine(path);
        if (line != null) {
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}
