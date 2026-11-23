package br.com.kairos.galaxyy;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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

import br.com.kairos.galaxyy.db.AssetDbHelper;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class BibleActivity extends Activity {

    private static final String BIBLE_DB = "ARA.sqlite";

    private static class BookItem {
        final int id;
        final String name;

        BookItem(int id, String name) {
            this.id = id;
            this.name = name;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bible);

        spinnerBooks = (Spinner) findViewById(R.id.spinnerBooks);
        spinnerChapters = (Spinner) findViewById(R.id.spinnerChapters);
        scrollBible = (ScrollView) findViewById(R.id.scrollBible);
        txtBibleChapter = (TextView) findViewById(R.id.txtBibleChapter);

        Button btnPrev = (Button) findViewById(R.id.btnPrevChapter);
        Button btnNext = (Button) findViewById(R.id.btnNextChapter);

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateChapter(-1);
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    loadChapters(books.get(position).id, pendingChapterIndex);
                    pendingChapterIndex = 0;
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
    }

    private void loadChapters(int bookId, int targetChapterIndex) {
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
            if (idx < 0) {
                idx = chapters.size() - 1;
            } else if (idx >= chapters.size()) {
                idx = 0;
            }
            spinnerChapters.setSelection(idx);
        }
    }

    private void loadVerses(BookItem book, int chapter) {
        StringBuilder sb = new StringBuilder();
        sb.append(book.name).append(" ").append(chapter).append("\n\n");

        Cursor cursor = null;
        try {
            cursor = bibleDb.rawQuery(
                    "SELECT verse, text FROM verse WHERE book_id = ? AND chapter = ? ORDER BY verse",
                    new String[]{String.valueOf(book.id), String.valueOf(chapter)}
            );
            while (cursor.moveToNext()) {
                int verseNum = cursor.getInt(0);
                String text = cursor.getString(1);
                sb.append(verseNum).append(". ").append(text).append("\n\n");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        txtBibleChapter.setText(sb.toString().trim());
        scrollBible.post(new Runnable() {
            @Override
            public void run() {
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
