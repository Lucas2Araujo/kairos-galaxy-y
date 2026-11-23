package br.com.kairos.galaxyy.db;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import br.com.kairos.galaxyy.R;

public class AssetDbHelper {

    public interface Callback {
        void onOpened(SQLiteDatabase db);
        void onError(Exception e);
    }

    public static boolean isDatabaseReady(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists() && dbFile.length() > 0;
    }

    public static SQLiteDatabase openAssetDatabase(Context context, String dbName) throws IOException {
        File dbFile = context.getDatabasePath(dbName);

        if (!dbFile.exists() || dbFile.length() == 0) {
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            File tmpFile = new File(parent, dbName + ".tmp");
            copyAssetFile(context, dbName, tmpFile);
            if (dbFile.exists()) {
                dbFile.delete();
            }
            if (!tmpFile.renameTo(dbFile)) {
                tmpFile.delete();
                throw new IOException("Falha ao mover banco temporario: " + dbName);
            }
        }

        return SQLiteDatabase.openDatabase(
                dbFile.getAbsolutePath(),
                null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
        );
    }

    public static AsyncTask<Void, Void, SQLiteDatabase> openAsync(
            final Activity activity,
            final String dbName,
            final Callback callback
    ) {
        final boolean needsCopy = !isDatabaseReady(activity, dbName);
        final ProgressDialog dialog = needsCopy ? new ProgressDialog(activity) : null;

        AsyncTask<Void, Void, SQLiteDatabase> task = new AsyncTask<Void, Void, SQLiteDatabase>() {
            private Exception error;

            @Override
            protected void onPreExecute() {
                if (dialog != null && !activity.isFinishing()) {
                    dialog.setMessage(activity.getString(R.string.loading_db));
                    dialog.setCancelable(false);
                    dialog.show();
                }
            }

            @Override
            protected SQLiteDatabase doInBackground(Void... params) {
                try {
                    return openAssetDatabase(activity.getApplicationContext(), dbName);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(SQLiteDatabase db) {
                if (dialog != null && dialog.isShowing()) {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {
                    }
                }
                if (activity.isFinishing()) {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                    return;
                }
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onOpened(db);
                }
            }
        };
        task.execute();
        return task;
    }

    private static void copyAssetFile(Context context, String assetName, File destFile) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = context.getAssets().open(assetName);
            out = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
            if (out != null) {
                try { out.close(); } catch (IOException ignored) {}
            }
        }
    }
}
