package br.com.kairos.galaxyy;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.kairos.galaxyy.db.AssetDbHelper;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class HymnalActivity extends Activity {

    public static final String HYMNAL_DB = "hinario_lite.db";

    private static class HymnItem {
        final int id;
        final String numero;
        final String titulo;

        HymnItem(int id, String numero, String titulo) {
            this.id = id;
            this.numero = numero;
            this.titulo = titulo;
        }

        @Override
        public String toString() {
            return numero + " - " + titulo;
        }
    }

    private SQLiteDatabase hymnalDb;
    private EditText editHymnNumber;
    private ListView listHymns;

    private final List<HymnItem> hymns = new ArrayList<HymnItem>();
    private final Map<String, HymnItem> hymnsByNumber = new HashMap<String, HymnItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hymnal);

        editHymnNumber = (EditText) findViewById(R.id.editHymnNumber);
        Button btnGoHymn = (Button) findViewById(R.id.btnGoHymn);
        listHymns = (ListView) findViewById(R.id.listHymns);

        btnGoHymn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpToHymnNumber();
            }
        });

        editHymnNumber.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    jumpToHymnNumber();
                    return true;
                }
                return false;
            }
        });

        listHymns.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < hymns.size()) {
                    openHymn(hymns.get(position));
                }
            }
        });

        AssetDbHelper.openAsync(this, HYMNAL_DB, new AssetDbHelper.Callback() {
            @Override
            public void onOpened(SQLiteDatabase db) {
                hymnalDb = db;
                loadHymnList();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(
                        HymnalActivity.this,
                        getString(R.string.error_open_hymnal) + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void loadHymnList() {
        hymns.clear();
        hymnsByNumber.clear();

        Cursor cursor = null;
        try {
            cursor = hymnalDb.rawQuery(
                    "SELECT id, numero, titulo FROM hino ORDER BY CAST(numero AS INTEGER), numero",
                    null
            );
            while (cursor.moveToNext()) {
                HymnItem item = new HymnItem(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2)
                );
                hymns.add(item);
                if (item.numero != null) {
                    hymnsByNumber.put(item.numero.trim(), item);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        ArrayAdapter<HymnItem> adapter = new ArrayAdapter<HymnItem>(
                this,
                android.R.layout.simple_list_item_1,
                hymns
        );
        listHymns.setAdapter(adapter);
    }

    private void jumpToHymnNumber() {
        String raw = editHymnNumber.getText().toString().trim();
        if (raw.length() == 0) {
            return;
        }
        String normalized = raw.replaceFirst("^0+(?!$)", "");
        HymnItem found = hymnsByNumber.get(normalized);
        if (found == null) {
            found = hymnsByNumber.get(raw);
        }
        if (found != null) {
            editHymnNumber.setText("");
            openHymn(found);
        } else {
            Toast.makeText(this, R.string.hymn_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void openHymn(HymnItem item) {
        Intent intent = new Intent(this, HymnReaderActivity.class);
        intent.putExtra(HymnReaderActivity.EXTRA_HYMN_ID, item.id);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hymnalDb != null && hymnalDb.isOpen()) {
            hymnalDb.close();
        }
    }
}
