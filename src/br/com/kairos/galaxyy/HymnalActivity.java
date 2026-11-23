package br.com.kairos.galaxyy;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Set;

import br.com.kairos.galaxyy.data.BibleReferenceParser;
import br.com.kairos.galaxyy.data.FavoritesHelper;
import br.com.kairos.galaxyy.db.AssetDbHelper;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class HymnalActivity extends Activity {

    public static final String HYMNAL_NEW_DB = "hinario_lite.db";
    public static final String HYMNAL_OLD_DB = "hinario_antigo_lite.db";

    private static class HymnItem {
        final int id;
        final String numero;
        final String titulo;
        final String normalizedTitle;

        HymnItem(int id, String numero, String titulo) {
            this.id = id;
            this.numero = numero;
            this.titulo = titulo;
            this.normalizedTitle = BibleReferenceParser.normalize(titulo);
        }

        @Override
        public String toString() {
            return numero + " - " + titulo;
        }
    }

    private SQLiteDatabase hymnalDb;
    private String activeDbName;
    private boolean favoritesOnly = false;

    private Button btnTabNew;
    private Button btnTabOld;
    private Button btnTabFavs;
    private EditText editHymnSearch;
    private TextView txtEmptyHymns;
    private ListView listHymns;

    private final List<HymnItem> allHymns = new ArrayList<HymnItem>();
    private final List<HymnItem> filteredHymns = new ArrayList<HymnItem>();
    private final Map<String, HymnItem> hymnsByNumber = new HashMap<String, HymnItem>();
    private ArrayAdapter<HymnItem> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hymnal);

        findViewById(R.id.rootHymnal).setBackgroundColor(ThemeHelper.getBackgroundColor(this));

        ThemeHelper.styleTopBar(
                this,
                findViewById(R.id.topBar),
                findViewById(R.id.topDivider),
                (TextView) findViewById(R.id.txtTopTitle),
                (Button) findViewById(R.id.btnBack)
        );

        btnTabNew = (Button) findViewById(R.id.btnTabNew);
        btnTabOld = (Button) findViewById(R.id.btnTabOld);
        btnTabFavs = (Button) findViewById(R.id.btnTabFavs);
        editHymnSearch = (EditText) findViewById(R.id.editHymnSearch);
        Button btnGoHymn = (Button) findViewById(R.id.btnGoHymn);
        txtEmptyHymns = (TextView) findViewById(R.id.txtEmptyHymns);
        listHymns = (ListView) findViewById(R.id.listHymns);

        ThemeHelper.stylePrimaryButton(this, btnGoHymn);
        txtEmptyHymns.setTextColor(ThemeHelper.getSecondaryTextColor(this));

        listAdapter = new ArrayAdapter<HymnItem>(
                this,
                android.R.layout.simple_list_item_1,
                filteredHymns
        );
        listHymns.setAdapter(listAdapter);

        btnTabNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritesOnly = false;
                switchDatabase(HYMNAL_NEW_DB);
            }
        });

        btnTabOld.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritesOnly = false;
                switchDatabase(HYMNAL_OLD_DB);
            }
        });

        btnTabFavs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritesOnly = !favoritesOnly;
                updateTabStyles();
                applyFilter();
            }
        });

        btnGoHymn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpOrOpenFirst();
            }
        });

        editHymnSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    jumpOrOpenFirst();
                    return true;
                }
                return false;
            }
        });

        editHymnSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listHymns.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < filteredHymns.size()) {
                    openHymn(filteredHymns.get(position));
                }
            }
        });

        String savedDb = FavoritesHelper.getActiveHymnalDb(this, HYMNAL_NEW_DB);
        switchDatabase(savedDb);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (favoritesOnly && !allHymns.isEmpty()) {
            applyFilter();
        }
    }

    private void updateTabStyles() {
        boolean isNew = HYMNAL_NEW_DB.equals(activeDbName);
        if (favoritesOnly) {
            ThemeHelper.styleBarButton(this, btnTabNew);
            ThemeHelper.styleBarButton(this, btnTabOld);
            ThemeHelper.stylePrimaryButton(this, btnTabFavs);
        } else if (isNew) {
            ThemeHelper.stylePrimaryButton(this, btnTabNew);
            ThemeHelper.styleBarButton(this, btnTabOld);
            ThemeHelper.styleBarButton(this, btnTabFavs);
        } else {
            ThemeHelper.styleBarButton(this, btnTabNew);
            ThemeHelper.stylePrimaryButton(this, btnTabOld);
            ThemeHelper.styleBarButton(this, btnTabFavs);
        }
    }

    private void switchDatabase(final String dbName) {
        if (dbName.equals(activeDbName) && hymnalDb != null && hymnalDb.isOpen()) {
            updateTabStyles();
            applyFilter();
            return;
        }
        activeDbName = dbName;
        FavoritesHelper.setActiveHymnalDb(this, dbName);
        updateTabStyles();

        if (hymnalDb != null && hymnalDb.isOpen()) {
            hymnalDb.close();
            hymnalDb = null;
        }

        AssetDbHelper.openAsync(this, dbName, new AssetDbHelper.Callback() {
            @Override
            public void onOpened(SQLiteDatabase db) {
                if (!dbName.equals(activeDbName)) {
                    db.close();
                    return;
                }
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
        allHymns.clear();
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
                allHymns.add(item);
                if (item.numero != null) {
                    hymnsByNumber.put(item.numero.trim(), item);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        applyFilter();
    }

    private void applyFilter() {
        String raw = editHymnSearch.getText().toString().trim();
        String queryNorm = BibleReferenceParser.normalize(raw);
        Set<Integer> favs = favoritesOnly ? FavoritesHelper.getFavorites(this, activeDbName) : null;

        filteredHymns.clear();
        for (int i = 0; i < allHymns.size(); i++) {
            HymnItem item = allHymns.get(i);
            if (favs != null && !favs.contains(Integer.valueOf(item.id))) {
                continue;
            }
            if (queryNorm.length() == 0
                    || item.numero.startsWith(raw)
                    || item.normalizedTitle.indexOf(queryNorm) >= 0) {
                filteredHymns.add(item);
            }
        }
        listAdapter.notifyDataSetChanged();

        if (filteredHymns.isEmpty() && favoritesOnly && queryNorm.length() == 0) {
            txtEmptyHymns.setVisibility(View.VISIBLE);
        } else {
            txtEmptyHymns.setVisibility(View.GONE);
        }
    }

    private void jumpOrOpenFirst() {
        String raw = editHymnSearch.getText().toString().trim();
        if (raw.length() == 0) {
            return;
        }
        String normalizedNum = raw.replaceFirst("^0+(?!$)", "");
        HymnItem exact = hymnsByNumber.get(normalizedNum);
        if (exact == null) {
            exact = hymnsByNumber.get(raw);
        }
        if (exact != null) {
            editHymnSearch.setText("");
            openHymn(exact);
            return;
        }
        if (!filteredHymns.isEmpty()) {
            openHymn(filteredHymns.get(0));
        } else {
            Toast.makeText(this, R.string.hymn_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void openHymn(HymnItem item) {
        Intent intent = new Intent(this, HymnReaderActivity.class);
        intent.putExtra(HymnReaderActivity.EXTRA_HYMN_ID, item.id);
        intent.putExtra(HymnReaderActivity.EXTRA_HYMNAL_DB, activeDbName);
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
