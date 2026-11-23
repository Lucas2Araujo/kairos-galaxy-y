package br.com.kairos.galaxyy;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import br.com.kairos.galaxyy.db.AssetDbHelper;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class HymnReaderActivity extends Activity {

    public static final String EXTRA_HYMN_ID = "hymn_id";

    private SQLiteDatabase hymnalDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hymn_reader);

        final TextView txtHeader = (TextView) findViewById(R.id.txtHymnHeader);
        final TextView txtLyrics = (TextView) findViewById(R.id.txtHymnLyrics);
        final int hymnId = getIntent().getIntExtra(EXTRA_HYMN_ID, -1);

        if (hymnId < 0) {
            finish();
            return;
        }

        AssetDbHelper.openAsync(this, HymnalActivity.HYMNAL_DB, new AssetDbHelper.Callback() {
            @Override
            public void onOpened(SQLiteDatabase db) {
                hymnalDb = db;
                loadHymn(hymnId, txtHeader, txtLyrics);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(
                        HymnReaderActivity.this,
                        getString(R.string.error_open_hymnal) + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void loadHymn(int hymnId, TextView txtHeader, TextView txtLyrics) {
        Cursor cursor = null;
        try {
            cursor = hymnalDb.rawQuery(
                    "SELECT numero, titulo, letra, autor_letra, texto_base FROM hino WHERE id = ?",
                    new String[]{String.valueOf(hymnId)}
            );
            if (cursor.moveToFirst()) {
                String numero = cursor.getString(0);
                String titulo = cursor.getString(1);
                String letra = cursor.getString(2);
                String autor = cursor.getString(3);
                String textoBase = cursor.getString(4);

                txtHeader.setText(numero + " - " + titulo);

                StringBuilder body = new StringBuilder();
                if (letra != null) {
                    body.append(letra.trim());
                }
                if (textoBase != null && textoBase.trim().length() > 0) {
                    body.append("\n\nTexto-base: ").append(textoBase.trim());
                }
                if (autor != null && autor.trim().length() > 0) {
                    body.append("\nLetra: ").append(autor.trim());
                }
                txtLyrics.setText(body.toString());
            } else {
                Toast.makeText(this, R.string.hymn_not_found, Toast.LENGTH_SHORT).show();
                finish();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hymnalDb != null && hymnalDb.isOpen()) {
            hymnalDb.close();
        }
    }
}
