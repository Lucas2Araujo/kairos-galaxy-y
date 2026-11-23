package br.com.kairos.galaxyy;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import br.com.kairos.galaxyy.data.FavoritesHelper;
import br.com.kairos.galaxyy.db.AssetDbHelper;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class HymnReaderActivity extends Activity {

    public static final String EXTRA_HYMN_ID = "hymn_id";
    public static final String EXTRA_HYMNAL_DB = "hymnal_db";

    private SQLiteDatabase hymnalDb;
    private String hymnalDbName;
    private int hymnId;

    private TextView txtTopTitle;
    private TextView txtHeader;
    private TextView txtLyrics;
    private Button btnFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_hymn_reader);

        findViewById(R.id.rootHymnReader).setBackgroundColor(ThemeHelper.getBackgroundColor(this));

        txtTopTitle = (TextView) findViewById(R.id.txtTopTitle);
        ThemeHelper.styleTopBar(
                this,
                findViewById(R.id.topBar),
                findViewById(R.id.topDivider),
                txtTopTitle,
                (Button) findViewById(R.id.btnBack)
        );

        btnFavorite = (Button) findViewById(R.id.btnFavorite);
        Button btnHymnInfo = (Button) findViewById(R.id.btnHymnInfo);
        Button btnFontDown = (Button) findViewById(R.id.btnFontDown);
        Button btnFontUp = (Button) findViewById(R.id.btnFontUp);

        ThemeHelper.styleBarButton(this, btnFavorite);
        ThemeHelper.styleBarButton(this, btnHymnInfo);
        ThemeHelper.styleBarButton(this, btnFontDown);
        ThemeHelper.styleBarButton(this, btnFontUp);

        txtHeader = (TextView) findViewById(R.id.txtHymnHeader);
        txtLyrics = (TextView) findViewById(R.id.txtHymnLyrics);

        txtHeader.setTextColor(ThemeHelper.getAccentColor(this));
        txtLyrics.setTextColor(ThemeHelper.getTextColor(this));
        applyFontSize();

        hymnId = getIntent().getIntExtra(EXTRA_HYMN_ID, -1);
        hymnalDbName = getIntent().getStringExtra(EXTRA_HYMNAL_DB);
        if (hymnalDbName == null || hymnalDbName.length() == 0) {
            hymnalDbName = HymnalActivity.HYMNAL_NEW_DB;
        }
        if (hymnId < 0) {
            finish();
            return;
        }

        updateFavoriteButton();

        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean added = FavoritesHelper.toggleFavorite(HymnReaderActivity.this, hymnalDbName, hymnId);
                updateFavoriteButton();
                Toast.makeText(
                        HymnReaderActivity.this,
                        added ? R.string.added_favorite : R.string.removed_favorite,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        btnHymnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HymnReaderActivity.this, HymnInfoActivity.class);
                intent.putExtra(EXTRA_HYMN_ID, hymnId);
                intent.putExtra(EXTRA_HYMNAL_DB, hymnalDbName);
                startActivity(intent);
            }
        });

        btnFontDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.adjustFontSize(HymnReaderActivity.this, -ThemeHelper.FONT_STEP_SP);
                applyFontSize();
            }
        });

        btnFontUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.adjustFontSize(HymnReaderActivity.this, ThemeHelper.FONT_STEP_SP);
                applyFontSize();
            }
        });

        AssetDbHelper.openAsync(this, hymnalDbName, new AssetDbHelper.Callback() {
            @Override
            public void onOpened(SQLiteDatabase db) {
                hymnalDb = db;
                loadHymn();
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

    private void updateFavoriteButton() {
        boolean isFav = FavoritesHelper.isFavorite(this, hymnalDbName, hymnId);
        btnFavorite.setText(isFav ? R.string.btn_fav_on : R.string.btn_fav_off);
    }

    private void applyFontSize() {
        txtLyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, ThemeHelper.getFontSize(this));
    }

    private void loadHymn() {
        Cursor cursor = null;
        try {
            cursor = hymnalDb.rawQuery(
                    "SELECT numero, titulo, letra FROM hino WHERE id = ?",
                    new String[]{String.valueOf(hymnId)}
            );
            if (cursor.moveToFirst()) {
                String numero = cursor.getString(0);
                String titulo = cursor.getString(1);
                String letra = cursor.getString(2);

                txtTopTitle.setText("Hino " + numero);
                txtHeader.setText(numero + " - " + titulo);
                txtLyrics.setText(formatLyrics(letra));
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

    private CharSequence formatLyrics(String letra) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int accentColor = ThemeHelper.getAccentColor(this);

        if (letra != null && letra.trim().length() > 0) {
            String[] stanzas = letra.trim().split("\\n\\s*\\n");
            for (int i = 0; i < stanzas.length; i++) {
                String stanza = stanzas[i].trim();
                if (stanza.length() == 0) {
                    continue;
                }
                if (ssb.length() > 0) {
                    ssb.append("\n\n");
                }
                int start = ssb.length();
                ssb.append(stanza);
                int end = ssb.length();

                String lower = stanza.toLowerCase();
                if (lower.startsWith("coro") || lower.startsWith("refrão") || lower.startsWith("refrao")) {
                    ssb.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    int lineBreak = stanza.indexOf('\n');
                    int labelEnd = lineBreak > 0 ? start + lineBreak : end;
                    ssb.setSpan(new ForegroundColorSpan(accentColor), start, labelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new StyleSpan(Typeface.BOLD), start, labelEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return ssb;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hymnalDb != null && hymnalDb.isOpen()) {
            hymnalDb.close();
        }
    }
}
