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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import br.com.kairos.galaxyy.data.BibleReferenceParser;
import br.com.kairos.galaxyy.db.AssetDbHelper;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class HymnInfoActivity extends Activity {

    private SQLiteDatabase hymnalDb;
    private SQLiteDatabase bibleDb;

    private TextView txtInfoTitle;
    private TextView lblBaseText;
    private TextView txtBaseVerseContent;
    private TextView btnOpenBaseChapter;
    private TextView lblRelatedTexts;
    private LinearLayout relatedRefsContainer;
    private TextView lblHymnDetails;
    private TextView txtHymnMetadata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hymn_info);

        findViewById(R.id.rootHymnInfo).setBackgroundColor(ThemeHelper.getBackgroundColor(this));

        ThemeHelper.styleTopBar(
                this,
                findViewById(R.id.topBar),
                findViewById(R.id.topDivider),
                (TextView) findViewById(R.id.txtTopTitle),
                (Button) findViewById(R.id.btnBack)
        );

        txtInfoTitle = (TextView) findViewById(R.id.txtInfoTitle);
        lblBaseText = (TextView) findViewById(R.id.lblBaseText);
        txtBaseVerseContent = (TextView) findViewById(R.id.txtBaseVerseContent);
        btnOpenBaseChapter = (TextView) findViewById(R.id.btnOpenBaseChapter);
        lblRelatedTexts = (TextView) findViewById(R.id.lblRelatedTexts);
        relatedRefsContainer = (LinearLayout) findViewById(R.id.relatedRefsContainer);
        lblHymnDetails = (TextView) findViewById(R.id.lblHymnDetails);
        txtHymnMetadata = (TextView) findViewById(R.id.txtHymnMetadata);

        int accent = ThemeHelper.getAccentColor(this);
        int text = ThemeHelper.getTextColor(this);
        txtInfoTitle.setTextColor(accent);
        lblBaseText.setTextColor(accent);
        txtBaseVerseContent.setTextColor(text);
        lblRelatedTexts.setTextColor(accent);
        lblHymnDetails.setTextColor(accent);
        txtHymnMetadata.setTextColor(text);
        ThemeHelper.styleRefCard(this, btnOpenBaseChapter);

        final int hymnId = getIntent().getIntExtra(HymnReaderActivity.EXTRA_HYMN_ID, -1);
        String dbName = getIntent().getStringExtra(HymnReaderActivity.EXTRA_HYMNAL_DB);
        if (dbName == null || dbName.length() == 0) {
            dbName = HymnalActivity.HYMNAL_NEW_DB;
        }
        if (hymnId < 0) {
            finish();
            return;
        }

        AssetDbHelper.openAsync(this, dbName, new AssetDbHelper.Callback() {
            @Override
            public void onOpened(SQLiteDatabase db) {
                hymnalDb = db;
                openBibleAndLoadInfo(hymnId);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(
                        HymnInfoActivity.this,
                        getString(R.string.error_open_hymnal) + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void openBibleAndLoadInfo(final int hymnId) {
        AssetDbHelper.openAsync(this, BibleActivity.BIBLE_DB, new AssetDbHelper.Callback() {
            @Override
            public void onOpened(SQLiteDatabase db) {
                bibleDb = db;
                loadInfo(hymnId);
            }

            @Override
            public void onError(Exception e) {
                // Exibe metadados mesmo se a Biblia falhar
                loadInfo(hymnId);
            }
        });
    }

    private void loadInfo(int hymnId) {
        Cursor cursor = null;
        try {
            cursor = hymnalDb.rawQuery(
                    "SELECT numero, titulo, autor_letra, autor_musica, texto_base, "
                            + "categoria, subcategoria, textos_relacionados FROM hino WHERE id = ?",
                    new String[]{String.valueOf(hymnId)}
            );
            if (!cursor.moveToFirst()) {
                finish();
                return;
            }
            String numero = cursor.getString(0);
            String titulo = cursor.getString(1);
            String autorLetra = cursor.getString(2);
            String autorMusica = cursor.getString(3);
            final String textoBase = cursor.getString(4);
            String categoria = cursor.getString(5);
            String subcategoria = cursor.getString(6);
            String textosRel = cursor.getString(7);

            txtInfoTitle.setText("Hino " + numero + " - " + titulo);

            // 1. Texto-base com versiculos completos direto da Biblia ARA
            if (textoBase != null && textoBase.trim().length() > 0) {
                final String cleanRef = textoBase.trim();
                lblBaseText.setText(getString(R.string.label_base_text) + " (" + cleanRef + ")");
                lblBaseText.setVisibility(View.VISIBLE);

                CharSequence verseBody = fetchBaseVerses(cleanRef);
                if (verseBody != null && verseBody.length() > 0) {
                    txtBaseVerseContent.setText(verseBody);
                    txtBaseVerseContent.setVisibility(View.VISIBLE);
                }

                btnOpenBaseChapter.setVisibility(View.VISIBLE);
                btnOpenBaseChapter.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openBibleRef(cleanRef);
                    }
                });
            }

            // 2. Textos relacionados clicaveis em grade de 2 colunas com chips curtos
            if (textosRel != null && textosRel.trim().length() > 0) {
                String[] rawRefs = textosRel.split("\\|");
                java.util.List<String> cleanRefs = new java.util.ArrayList<String>();
                for (int i = 0; i < rawRefs.length; i++) {
                    String ref = rawRefs[i].trim();
                    if (ref.length() > 0) {
                        cleanRefs.add(ref);
                    }
                }
                if (!cleanRefs.isEmpty()) {
                    lblRelatedTexts.setVisibility(View.VISIBLE);
                    int gapPx = dpToPx(6);
                    int minH = dpToPx(44);
                    int padH = dpToPx(8);
                    int padV = dpToPx(8);

                    for (int i = 0; i < cleanRefs.size(); i += 2) {
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.FILL_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        rowLp.bottomMargin = gapPx;
                        row.setLayoutParams(rowLp);

                        row.addView(createRefChip(cleanRefs.get(i), minH, padH, padV, 0));

                        if (i + 1 < cleanRefs.size()) {
                            row.addView(createRefChip(cleanRefs.get(i + 1), minH, padH, padV, gapPx));
                        }
                        relatedRefsContainer.addView(row);
                    }
                }
            }

            // 3. Metadados do Hino
            StringBuilder meta = new StringBuilder();
            appendMeta(meta, "Categoria", categoria);
            appendMeta(meta, "Subcategoria", subcategoria);
            appendMeta(meta, "Letra", autorLetra);
            appendMeta(meta, "Música", autorMusica);
            if (meta.length() == 0) {
                meta.append("Sem metadados adicionais.");
            }
            txtHymnMetadata.setText(meta.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void appendMeta(StringBuilder sb, String label, String value) {
        if (value != null && value.trim().length() > 0) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(label).append(": ").append(value.trim());
        }
    }

    private CharSequence fetchBaseVerses(String rawRef) {
        if (bibleDb == null || !bibleDb.isOpen()) {
            return null;
        }
        BibleReferenceParser.ParsedRef parsed = BibleReferenceParser.parse(rawRef);
        if (parsed == null) {
            return null;
        }

        int bookId = -1;
        Cursor bCur = null;
        try {
            bCur = bibleDb.rawQuery("SELECT id, name FROM book ORDER BY id", null);
            while (bCur.moveToNext()) {
                int id = bCur.getInt(0);
                String norm = BibleReferenceParser.normalize(bCur.getString(1));
                if (norm.equals(parsed.normalizedBook)) {
                    bookId = id;
                    break;
                }
                if (bookId < 0 && norm.startsWith(parsed.normalizedBook)) {
                    bookId = id;
                }
            }
        } finally {
            if (bCur != null) {
                bCur.close();
            }
        }
        if (bookId < 0) {
            return null;
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int accent = ThemeHelper.getAccentColor(this);
        Cursor vCur = null;
        try {
            String sql;
            String[] args;
            if (parsed.verseStart > 0) {
                sql = "SELECT verse, text FROM verse WHERE book_id = ? AND chapter = ? "
                        + "AND verse >= ? AND verse <= ? ORDER BY verse";
                args = new String[]{
                        String.valueOf(bookId),
                        String.valueOf(parsed.chapter),
                        String.valueOf(parsed.verseStart),
                        String.valueOf(parsed.verseEnd)
                };
            } else {
                sql = "SELECT verse, text FROM verse WHERE book_id = ? AND chapter = ? ORDER BY verse LIMIT 5";
                args = new String[]{String.valueOf(bookId), String.valueOf(parsed.chapter)};
            }
            vCur = bibleDb.rawQuery(sql, args);
            while (vCur.moveToNext()) {
                if (ssb.length() > 0) {
                    ssb.append("\n");
                }
                int start = ssb.length();
                String numStr = vCur.getInt(0) + " ";
                ssb.append(numStr);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), start, start + numStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(accent), start, start + numStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append(vCur.getString(1));
            }
        } finally {
            if (vCur != null) {
                vCur.close();
            }
        }
        return ssb;
    }

    private int dpToPx(int dp) {
        return (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private TextView createRefChip(final String ref, int minH, int padH, int padV, int leftMargin) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        lp.leftMargin = leftMargin;
        chip.setLayoutParams(lp);
        chip.setMinHeight(minH);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding(padH, padV, padH, padV);
        chip.setTextSize(14);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setText(ref);
        ThemeHelper.styleRefCard(this, chip);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBibleRef(ref);
            }
        });
        return chip;
    }

    private void openBibleRef(String ref) {
        Intent intent = new Intent(this, BibleActivity.class);
        intent.putExtra(BibleActivity.EXTRA_BIBLE_REF, ref);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hymnalDb != null && hymnalDb.isOpen()) {
            hymnalDb.close();
        }
        if (bibleDb != null && bibleDb.isOpen()) {
            bibleDb.close();
        }
    }
}
