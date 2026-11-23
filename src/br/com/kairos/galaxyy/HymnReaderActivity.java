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
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import br.com.kairos.galaxyy.db.AssetDbHelper;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class HymnReaderActivity extends Activity {

    public static final String EXTRA_HYMN_ID = "hymn_id";

    private SQLiteDatabase hymnalDb;
    private TextView txtTopTitle;
    private TextView txtHeader;
    private TextView txtBibleRefCard;
    private TextView txtLyrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hymn_reader);

        View root = findViewById(R.id.rootHymnReader);
        root.setBackgroundColor(ThemeHelper.getBackgroundColor(this));

        txtTopTitle = (TextView) findViewById(R.id.txtTopTitle);
        ThemeHelper.styleTopBar(
                this,
                findViewById(R.id.topBar),
                findViewById(R.id.topDivider),
                txtTopTitle,
                (Button) findViewById(R.id.btnBack)
        );

        Button btnFontDown = (Button) findViewById(R.id.btnFontDown);
        Button btnFontUp = (Button) findViewById(R.id.btnFontUp);
        ThemeHelper.styleBarButton(this, btnFontDown);
        ThemeHelper.styleBarButton(this, btnFontUp);

        txtHeader = (TextView) findViewById(R.id.txtHymnHeader);
        txtBibleRefCard = (TextView) findViewById(R.id.txtBibleRefCard);
        txtLyrics = (TextView) findViewById(R.id.txtHymnLyrics);

        txtHeader.setTextColor(ThemeHelper.getAccentColor(this));
        txtLyrics.setTextColor(ThemeHelper.getTextColor(this));
        ThemeHelper.styleRefCard(this, txtBibleRefCard);
        applyFontSize();

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

        final int hymnId = getIntent().getIntExtra(EXTRA_HYMN_ID, -1);
        if (hymnId < 0) {
            finish();
            return;
        }

        AssetDbHelper.openAsync(this, HymnalActivity.HYMNAL_DB, new AssetDbHelper.Callback() {
            @Override
            public void onOpened(SQLiteDatabase db) {
                hymnalDb = db;
                loadHymn(hymnId);
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

    private void applyFontSize() {
        txtLyrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, ThemeHelper.getFontSize(this));
    }

    private void loadHymn(int hymnId) {
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
                final String textoBase = cursor.getString(4);

                txtTopTitle.setText("Hino " + numero);
                txtHeader.setText(numero + " - " + titulo);

                if (textoBase != null && textoBase.trim().length() > 0) {
                    final String cleanRef = textoBase.trim();
                    txtBibleRefCard.setText(getString(R.string.open_in_bible_prefix) + cleanRef + " >");
                    txtBibleRefCard.setVisibility(View.VISIBLE);
                    txtBibleRefCard.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(HymnReaderActivity.this, BibleActivity.class);
                            intent.putExtra(BibleActivity.EXTRA_BIBLE_REF, cleanRef);
                            startActivity(intent);
                        }
                    });
                } else {
                    txtBibleRefCard.setVisibility(View.GONE);
                }

                txtLyrics.setText(formatLyrics(letra, autor));
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

    private CharSequence formatLyrics(String letra, String autor) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int accentColor = ThemeHelper.getAccentColor(this);
        int secondaryColor = ThemeHelper.getSecondaryTextColor(this);

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

        if (autor != null && autor.trim().length() > 0) {
            if (ssb.length() > 0) {
                ssb.append("\n\n");
            }
            int start = ssb.length();
            ssb.append("Letra: ").append(autor.trim());
            int end = ssb.length();
            ssb.setSpan(new ForegroundColorSpan(secondaryColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new RelativeSizeSpan(0.85f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
