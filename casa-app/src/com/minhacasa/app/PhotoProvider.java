package com.minhacasa.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Guarda as fotos tiradas pela câmera para a tela "Criar cômodo com fotos". */
public class PhotoProvider extends ContentProvider {
    static final String AUTHORITY = "com.minhacasa.app.photos";

    static Uri newPhotoUri() {
        return Uri.parse("content://" + AUTHORITY + "/foto_" + System.currentTimeMillis() + ".jpg");
    }

    private static File dir(Context ctx) {
        File d = new File(ctx.getCacheDir(), "photos");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static File fileFor(Context ctx, Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || !name.matches("[A-Za-z0-9_.-]+")) throw new FileNotFoundException("nome inválido");
        return new File(dir(ctx), name);
    }

    static boolean exists(Context ctx, Uri uri) {
        try {
            File f = fileFor(ctx, uri);
            return f.exists() && f.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Apaga fotos com mais de um dia. */
    static void cleanup(Context ctx) {
        File[] files = dir(ctx).listFiles();
        if (files == null) return;
        long limit = System.currentTimeMillis() - 24L * 3600 * 1000;
        for (File f : files) if (f.lastModified() < limit) f.delete();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(fileFor(getContext(), uri), ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        File f;
        try {
            f = fileFor(getContext(), uri);
        } catch (FileNotFoundException e) {
            return null;
        }
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(), f.length()});
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
