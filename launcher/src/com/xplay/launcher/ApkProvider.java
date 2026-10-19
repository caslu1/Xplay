package com.xplay.launcher;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Shares a downloaded APK with the system installer (content:// is required on Android 7+). */
public class ApkProvider extends ContentProvider {

    static final String AUTHORITY = "com.xplay.launcher.apk";

    static File dir(Context c) {
        // Android 6 and older install from a file:// address, which must be readable by the installer.
        return Build.VERSION.SDK_INT >= 24 || c.getExternalCacheDir() == null
                ? new File(c.getCacheDir(), "apk") : c.getExternalCacheDir();
    }

    static File file(Context c, String name) {
        return new File(dir(c), name.replaceAll("[^A-Za-z0-9._-]", "_") + ".apk");
    }

    static Uri uri(File f) {
        return Uri.parse("content://" + AUTHORITY + "/" + f.getName());
    }

    private File resolve(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("/") || name.contains("..")) throw new FileNotFoundException();
        return new File(dir(getContext()), name);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        MatrixCursor c = new MatrixCursor(new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        try {
            File f = resolve(uri);
            c.addRow(new Object[] {f.getName(), f.length()});
        } catch (FileNotFoundException ignored) {
            // Empty cursor.
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}
