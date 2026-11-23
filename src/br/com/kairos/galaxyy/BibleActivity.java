package br.com.kairos.galaxyy;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.kairos.galaxyy.data.BibleReferenceParser;
import br.com.kairos.galaxyy.data.FavoritesHelper;
import br.com.kairos.galaxyy.db.AssetDbHelper;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class BibleActivity extends Activity {

    public static final String EXTRA_BIBLE_REF = "bible_ref";
    public static final String BIBLE_DB = "ARA.sqlite";
    public static final String PERICOPES_DB = "pericopes.sqlite";
    private static final int REQ_PICK_BIBLE = 101;

    private static class BookItem {
        final int id;
        final String name;
        final String normalizedName;

        BookItem(int id, String name) {
            this.id = id;
            this.name = name;
            this.normalizedName = BibleReferenceParser.normalize(name);
        }
    }

    private SQLiteDatabase bibleDb;
    private SQLiteDatabase pericopesDb;
    private Button btnPickReference;
    private ScrollView scrollBible;
    private TextView txtBibleChapter;

    private final List<BookItem> books = new ArrayList<BookItem>();
    private final List<Integer> chapters = new ArrayList<Integer>();

    private int currentBookIndex = 0;
    private int currentChapterIndex = 0;
    private int highlightVerseStart = -1;
    private int highlightVerseEnd = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_bible);

        findViewById(R.id.rootBible).setBackgroundColor(ThemeHelper.getBackgroundColor(this));

        ThemeHelper.styleTopBar(
                this,
                findViewById(R.id.topBar),
                findViewById(R.id.topDivider),
                (TextView) findViewById(R.id.txtTopTitle),
                (Button) findViewById(R.id.btnBack)
        );

        Button btnFontDown = (Button) findViewById(R.id.btnFontDown);
        Button btnFontUp = (Button) findViewById(R.id.btnFontUp);
        Button btnPrev = (Button) findViewById(R.id.btnPrevChapter);
        Button btnNext = (Button) findViewById(R.id.btnNextChapter);
        btnPickReference = (Button) findViewById(R.id.btnPickReference);

        ThemeHelper.styleBarButton(this, btnFontDown);
        ThemeHelper.styleBarButton(this, btnFontUp);
        ThemeHelper.styleBarButton(this, btnPrev);
        ThemeHelper.styleBarButton(this, btnNext);
        ThemeHelper.stylePrimaryButton(this, btnPickReference);

        scrollBible = (ScrollView) findViewById(R.id.scrollBible);
        txtBibleChapter = (TextView) findViewById(R.id.txtBibleChapter);
        txtBibleChapter.setTextColor(ThemeHelper.getTextColor(this));
        applyFontSize();

        btnFontDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.adjustFontSize(BibleActivity.this, -ThemeHelper.FONT_STEP_SP);
                applyFontSize();
            }
        });

        btnFontUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.adjustFontSize(BibleActivity.this, ThemeHelper.FONT_STEP_SP);
                applyFontSize();
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHighlight();
                navigateChapter(-1);
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHighlight();
                navigateChapter(1);
            }
        });

        btnPickReference.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (books.isEmpty()) {
                    return;
                }
                Intent intent = new Intent(BibleActivity.this, BiblePickerActivity.class);
                intent.putExtra(BiblePickerActivity.EXTRA_BOOK_ID, books.get(currentBookIndex).id);
                startActivityForResult(intent, REQ_PICK_BIBLE);
            }
        });

        AssetDbHelper.openAsync(this, BIBLE_DB, new AssetDbHelper.Callback() {
            @Override
            public void onOpened(SQLiteDatabase db) {
                bibleDb = db;
                openPericopesOptional();
                loadBooksAndInitialChapter();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(
                        BibleActivity.this,
                        getString(R.string.error_open_bible) + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void openPericopesOptional() {
        try {
            pericopesDb = AssetDbHelper.openAssetDatabase(getApplicationContext(), PERICOPES_DB);
        } catch (Exception ignored) {
            pericopesDb = null;
        }
    }

    private void applyFontSize() {
        txtBibleChapter.setTextSize(TypedValue.COMPLEX_UNIT_SP, ThemeHelper.getFontSize(this));
    }

    private void clearHighlight() {
        highlightVerseStart = -1;
        highlightVerseEnd = -1;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_BIBLE && resultCode == RESULT_OK && data != null) {
            int bookId = data.getIntExtra(BiblePickerActivity.EXTRA_BOOK_ID, 1);
            int chapter = data.getIntExtra(BiblePickerActivity.EXTRA_CHAPTER, 1);
            clearHighlight();
            int idx = findBookIndexById(bookId);
            if (idx >= 0) {
                openBookAndChapter(idx, chapter, false);
            }
        }
    }

    private void loadBooksAndInitialChapter() {
        books.clear();
        Cursor cursor = null;
        try {
            cursor = bibleDb.rawQuery("SELECT id, name FROM book ORDER BY id", null);
            while (cursor.moveToNext()) {
                books.add(new BookItem(cursor.getInt(0), cursor.getString(1)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (books.isEmpty()) {
            return;
        }

        int targetBookIdx = 0;
        int targetChapter = 1;
        String incomingRef = getIntent().getStringExtra(EXTRA_BIBLE_REF);
        if (incomingRef != null && incomingRef.trim().length() > 0) {
            BibleReferenceParser.ParsedRef parsed = BibleReferenceParser.parse(incomingRef);
            int matchedIndex = findBookIndex(parsed);
            if (parsed != null && matchedIndex >= 0) {
                targetBookIdx = matchedIndex;
                targetChapter = parsed.chapter;
                highlightVerseStart = parsed.verseStart;
                highlightVerseEnd = parsed.verseEnd;
            } else {
                Toast.makeText(this, R.string.ref_not_found, Toast.LENGTH_SHORT).show();
            }
        } else {
            int lastBookId = FavoritesHelper.getLastBibleBookId(this);
            int lastChapter = FavoritesHelper.getLastBibleChapter(this);
            int idx = findBookIndexById(lastBookId);
            if (idx >= 0) {
                targetBookIdx = idx;
                targetChapter = lastChapter;
            }
        }

        openBookAndChapter(targetBookIdx, targetChapter, false);
    }

    private int findBookIndexById(int bookId) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).id == bookId) {
                return i;
            }
        }
        return -1;
    }

    private int findBookIndex(BibleReferenceParser.ParsedRef parsed) {
        if (parsed == null) {
            return -1;
        }
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).normalizedName.equals(parsed.normalizedBook)) {
                return i;
            }
        }
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).normalizedName.startsWith(parsed.normalizedBook)) {
                return i;
            }
        }
        return -1;
    }

    private void openBookAndChapter(int bookIndex, int chapterNumber, boolean selectLastChapter) {
        currentBookIndex = bookIndex;
        BookItem book = books.get(currentBookIndex);

        chapters.clear();
        Cursor cursor = null;
        try {
            cursor = bibleDb.rawQuery(
                    "SELECT DISTINCT chapter FROM verse WHERE book_id = ? ORDER BY chapter",
                    new String[]{String.valueOf(book.id)}
            );
            while (cursor.moveToNext()) {
                chapters.add(Integer.valueOf(cursor.getInt(0)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (chapters.isEmpty()) {
            return;
        }

        if (selectLastChapter) {
            currentChapterIndex = chapters.size() - 1;
        } else {
            int found = chapters.indexOf(Integer.valueOf(chapterNumber));
            currentChapterIndex = found >= 0 ? found : 0;
        }

        int chapter = chapters.get(currentChapterIndex).intValue();
        btnPickReference.setText(book.name + " " + chapter + " ▾");
        FavoritesHelper.saveLastBiblePosition(this, book.id, chapter);
        loadVerses(book, chapter);
    }

    private Map<Integer, String> loadPericopesForChapter(int bookId, int chapter) {
        Map<Integer, String> map = new HashMap<Integer, String>();
        if (pericopesDb == null || !pericopesDb.isOpen()) {
            return map;
        }
        Cursor cursor = null;
        try {
            cursor = pericopesDb.rawQuery(
                    "SELECT verse, title FROM pericope WHERE book_id = ? AND chapter = ? ORDER BY verse",
                    new String[]{String.valueOf(bookId), String.valueOf(chapter)}
            );
            while (cursor.moveToNext()) {
                int v = cursor.getInt(0);
                String t = cursor.getString(1);
                if (t != null && t.trim().length() > 0 && !map.containsKey(Integer.valueOf(v))) {
                    map.put(Integer.valueOf(v), t.trim());
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return map;
    }

    private void loadVerses(BookItem book, int chapter) {
        int accentColor = ThemeHelper.getAccentColor(this);
        int highlightBg = ThemeHelper.getHighlightBgColor(this);
        Map<Integer, String> pericopes = loadPericopesForChapter(book.id, chapter);

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        String header = book.name + " " + chapter + "\n\n";
        ssb.append(header);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, header.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new ForegroundColorSpan(accentColor), 0, header.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new RelativeSizeSpan(1.15f), 0, header.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int firstHighlightOffset = -1;

        Cursor cursor = null;
        try {
            cursor = bibleDb.rawQuery(
                    "SELECT verse, text FROM verse WHERE book_id = ? AND chapter = ? ORDER BY verse",
                    new String[]{String.valueOf(book.id), String.valueOf(chapter)}
            );
            while (cursor.moveToNext()) {
                int verseNum = cursor.getInt(0);
                String text = cursor.getString(1);

                String pericope = pericopes.get(Integer.valueOf(verseNum));
                if (pericope != null) {
                    int pStart = ssb.length();
                    ssb.append(pericope).append("\n");
                    int pEnd = ssb.length();
                    ssb.setSpan(new StyleSpan(Typeface.BOLD), pStart, pEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new ForegroundColorSpan(accentColor), pStart, pEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.setSpan(new RelativeSizeSpan(0.92f), pStart, pEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                int verseStartOffset = ssb.length();
                String numStr = verseNum + " ";
                ssb.append(numStr);
                ssb.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        verseStartOffset,
                        verseStartOffset + numStr.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                ssb.setSpan(
                        new ForegroundColorSpan(accentColor),
                        verseStartOffset,
                        verseStartOffset + numStr.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                ssb.append(text);
                int verseEndOffset = ssb.length();

                if (highlightVerseStart > 0 && verseNum >= highlightVerseStart && verseNum <= highlightVerseEnd) {
                    if (firstHighlightOffset < 0) {
                        firstHighlightOffset = verseStartOffset;
                    }
                    ssb.setSpan(
                            new BackgroundColorSpan(highlightBg),
                            verseStartOffset,
                            verseEndOffset,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }

                ssb.append("\n\n");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        txtBibleChapter.setText(ssb);

        final int scrollTargetOffset = firstHighlightOffset;
        clearHighlight();

        scrollBible.post(new Runnable() {
            @Override
            public void run() {
                if (scrollTargetOffset >= 0) {
                    Layout layout = txtBibleChapter.getLayout();
                    if (layout != null) {
                        int line = layout.getLineForOffset(scrollTargetOffset);
                        int y = layout.getLineTop(line);
                        scrollBible.scrollTo(0, Math.max(0, y - 16));
                        return;
                    }
                }
                scrollBible.scrollTo(0, 0);
            }
        });
    }

    private void navigateChapter(int delta) {
        if (books.isEmpty() || chapters.isEmpty()) {
            return;
        }
        int nextChapIdx = currentChapterIndex + delta;
        if (nextChapIdx >= 0 && nextChapIdx < chapters.size()) {
            currentChapterIndex = nextChapIdx;
            BookItem book = books.get(currentBookIndex);
            int chapter = chapters.get(currentChapterIndex).intValue();
            btnPickReference.setText(book.name + " " + chapter + " ▾");
            FavoritesHelper.saveLastBiblePosition(this, book.id, chapter);
            loadVerses(book, chapter);
        } else if (delta > 0 && currentBookIndex + 1 < books.size()) {
            openBookAndChapter(currentBookIndex + 1, 1, false);
        } else if (delta < 0 && currentBookIndex - 1 >= 0) {
            openBookAndChapter(currentBookIndex - 1, 1, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bibleDb != null && bibleDb.isOpen()) {
            bibleDb.close();
        }
        if (pericopesDb != null && pericopesDb.isOpen()) {
            pericopesDb.close();
        }
    }
}
