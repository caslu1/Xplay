package com.xplay.iptv;

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

/** Shares the downloaded update with the system installer (content:// is required on Android 7+). */
public class UpdateProvider extends ContentProvider {

    static final String AUTHORITY = "com.xplay.iptv.update";

    static File apkFile(Context c) {
        // Android 6 and older install from a file:// address, which must be readable by the installer.
        File dir = Build.VERSION.SDK_INT >= 24 || c.getExternalCacheDir() == null
                ? new File(c.getCacheDir(), "update") : c.getExternalCacheDir();
        return new File(dir, "xplay-update.apk");
    }

    static Uri uri(Context c) {
        return Uri.parse("content://" + AUTHORITY + "/xplay-update.apk");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(apkFile(getContext()), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        File f = apkFile(getContext());
        MatrixCursor c = new MatrixCursor(new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[] {f.getName(), f.length()});
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
