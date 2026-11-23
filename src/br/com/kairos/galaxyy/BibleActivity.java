package br.com.kairos.galaxyy;

import android.app.Activity;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import br.com.kairos.galaxyy.data.BibleReferenceParser;
import br.com.kairos.galaxyy.db.AssetDbHelper;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class BibleActivity extends Activity {

    public static final String EXTRA_BIBLE_REF = "bible_ref";
    private static final String BIBLE_DB = "ARA.sqlite";

    private static class BookItem {
        final int id;
        final String name;
        final String normalizedName;

        BookItem(int id, String name) {
            this.id = id;
            this.name = name;
            this.normalizedName = BibleReferenceParser.normalize(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private SQLiteDatabase bibleDb;
    private Spinner spinnerBooks;
    private Spinner spinnerChapters;
    private ScrollView scrollBible;
    private TextView txtBibleChapter;

    private final List<BookItem> books = new ArrayList<BookItem>();
    private final List<Integer> chapters = new ArrayList<Integer>();

    private int pendingChapterIndex = 0;
    private int pendingChapterNumber = -1;
    private int highlightVerseStart = -1;
    private int highlightVerseEnd = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bible);

        View root = findViewById(R.id.rootBible);
        root.setBackgroundColor(ThemeHelper.getBackgroundColor(this));

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

        ThemeHelper.styleBarButton(this, btnFontDown);
        ThemeHelper.styleBarButton(this, btnFontUp);
        ThemeHelper.styleBarButton(this, btnPrev);
        ThemeHelper.styleBarButton(this, btnNext);

        spinnerBooks = (Spinner) findViewById(R.id.spinnerBooks);
        spinnerChapters = (Spinner) findViewById(R.id.spinnerChapters);
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

        AssetDbHelper.openAsync(this, BIBLE_DB, new AssetDbHelper.Callback() {
            @Override
            public void onOpened(SQLiteDatabase db) {
                bibleDb = db;
                loadBooks();
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

    private void applyFontSize() {
        txtBibleChapter.setTextSize(TypedValue.COMPLEX_UNIT_SP, ThemeHelper.getFontSize(this));
    }

    private void clearHighlight() {
        highlightVerseStart = -1;
        highlightVerseEnd = -1;
    }

    private void loadBooks() {
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

        int initialBookIndex = 0;
        String incomingRef = getIntent().getStringExtra(EXTRA_BIBLE_REF);
        if (incomingRef != null && incomingRef.trim().length() > 0) {
            BibleReferenceParser.ParsedRef parsed = BibleReferenceParser.parse(incomingRef);
            int matchedIndex = findBookIndex(parsed);
            if (parsed != null && matchedIndex >= 0) {
                initialBookIndex = matchedIndex;
                pendingChapterNumber = parsed.chapter;
                highlightVerseStart = parsed.verseStart;
                highlightVerseEnd = parsed.verseEnd;
            } else {
                Toast.makeText(this, R.string.ref_not_found, Toast.LENGTH_SHORT).show();
            }
        }

        ArrayAdapter<BookItem> adapter = new ArrayAdapter<BookItem>(
                this,
                android.R.layout.simple_spinner_item,
                books
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBooks.setAdapter(adapter);

        spinnerBooks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < books.size()) {
                    loadChapters(books.get(position).id, pendingChapterIndex, pendingChapterNumber);
                    pendingChapterIndex = 0;
                    pendingChapterNumber = -1;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerChapters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int bookPos = spinnerBooks.getSelectedItemPosition();
                if (bookPos >= 0 && bookPos < books.size() && position >= 0 && position < chapters.size()) {
                    loadVerses(books.get(bookPos), chapters.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (initialBookIndex > 0 && initialBookIndex < books.size()) {
            spinnerBooks.setSelection(initialBookIndex);
        }
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

    private void loadChapters(int bookId, int targetChapterIndex, int targetChapterNumber) {
        chapters.clear();
        Cursor cursor = null;
        try {
            cursor = bibleDb.rawQuery(
                    "SELECT DISTINCT chapter FROM verse WHERE book_id = ? ORDER BY chapter",
                    new String[]{String.valueOf(bookId)}
            );
            while (cursor.moveToNext()) {
                chapters.add(cursor.getInt(0));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(
                this,
                android.R.layout.simple_spinner_item,
                chapters
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChapters.setAdapter(adapter);

        if (!chapters.isEmpty()) {
            int idx = targetChapterIndex;
            if (targetChapterNumber > 0) {
                int found = chapters.indexOf(Integer.valueOf(targetChapterNumber));
                if (found >= 0) {
                    idx = found;
                }
            }
            if (idx < 0) {
                idx = chapters.size() - 1;
            } else if (idx >= chapters.size()) {
                idx = 0;
            }
            spinnerChapters.setSelection(idx);
        }
    }

    private void loadVerses(BookItem book, int chapter) {
        int accentColor = ThemeHelper.getAccentColor(this);
        int highlightBg = ThemeHelper.getHighlightBgColor(this);

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
        int bookPos = spinnerBooks.getSelectedItemPosition();
        int chapPos = spinnerChapters.getSelectedItemPosition();
        int nextChap = chapPos + delta;

        if (nextChap >= 0 && nextChap < chapters.size()) {
            spinnerChapters.setSelection(nextChap);
        } else if (delta > 0 && bookPos + 1 < books.size()) {
            pendingChapterIndex = 0;
            spinnerBooks.setSelection(bookPos + 1);
        } else if (delta < 0 && bookPos - 1 >= 0) {
            pendingChapterIndex = -1;
            spinnerBooks.setSelection(bookPos - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bibleDb != null && bibleDb.isOpen()) {
            bibleDb.close();
        }
    }
}
