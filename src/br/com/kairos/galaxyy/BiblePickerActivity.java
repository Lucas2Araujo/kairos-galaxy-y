package br.com.kairos.galaxyy;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import br.com.kairos.galaxyy.db.AssetDbHelper;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class BiblePickerActivity extends Activity {

    public static final String EXTRA_BOOK_ID = "selected_book_id";
    public static final String EXTRA_CHAPTER = "selected_chapter";

    private static class BookRow {
        final int id;
        final int testamentId;
        final String name;

        BookRow(int id, int testamentId, String name) {
            this.id = id;
            this.testamentId = testamentId;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private SQLiteDatabase bibleDb;
    private TextView txtTopTitle;
    private View testamentTabs;
    private Button btnTabAT;
    private Button btnTabNT;
    private ListView listBooks;
    private GridView gridChapters;

    private final List<BookRow> allBooks = new ArrayList<BookRow>();
    private final List<BookRow> visibleBooks = new ArrayList<BookRow>();
    private final List<Integer> chapters = new ArrayList<Integer>();

    private int activeTestament = 1;
    private BookRow selectedBook = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bible_picker);

        findViewById(R.id.rootPicker).setBackgroundColor(ThemeHelper.getBackgroundColor(this));

        txtTopTitle = (TextView) findViewById(R.id.txtTopTitle);
        Button btnBack = (Button) findViewById(R.id.btnBack);
        ThemeHelper.styleTopBar(
                this,
                findViewById(R.id.topBar),
                findViewById(R.id.topDivider),
                txtTopTitle,
                btnBack
        );

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackStep();
            }
        });

        testamentTabs = findViewById(R.id.testamentTabs);
        btnTabAT = (Button) findViewById(R.id.btnTabAT);
        btnTabNT = (Button) findViewById(R.id.btnTabNT);
        listBooks = (ListView) findViewById(R.id.listBooks);
        gridChapters = (GridView) findViewById(R.id.gridChapters);

        btnTabAT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTestament = 1;
                refreshBookList();
            }
        });

        btnTabNT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTestament = 2;
                refreshBookList();
            }
        });

        listBooks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < visibleBooks.size()) {
                    showChaptersForBook(visibleBooks.get(position));
                }
            }
        });

        gridChapters.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (selectedBook != null && position >= 0 && position < chapters.size()) {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_BOOK_ID, selectedBook.id);
                    data.putExtra(EXTRA_CHAPTER, chapters.get(position).intValue());
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });

        AssetDbHelper.openAsync(this, BibleActivity.BIBLE_DB, new AssetDbHelper.Callback() {
            @Override
            public void onOpened(SQLiteDatabase db) {
                bibleDb = db;
                loadAllBooks();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(
                        BiblePickerActivity.this,
                        getString(R.string.error_open_bible) + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void handleBackStep() {
        if (selectedBook != null) {
            selectedBook = null;
            gridChapters.setVisibility(View.GONE);
            testamentTabs.setVisibility(View.VISIBLE);
            listBooks.setVisibility(View.VISIBLE);
            txtTopTitle.setText(R.string.title_bible_picker);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackStep();
    }

    private void loadAllBooks() {
        allBooks.clear();
        Cursor cursor = null;
        try {
            cursor = bibleDb.rawQuery(
                    "SELECT id, testament_reference_id, name FROM book ORDER BY id",
                    null
            );
            while (cursor.moveToNext()) {
                allBooks.add(new BookRow(cursor.getInt(0), cursor.getInt(1), cursor.getString(2)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        int currentBookId = getIntent().getIntExtra(EXTRA_BOOK_ID, 1);
        for (int i = 0; i < allBooks.size(); i++) {
            if (allBooks.get(i).id == currentBookId) {
                activeTestament = allBooks.get(i).testamentId;
                break;
            }
        }
        refreshBookList();
    }

    private void refreshBookList() {
        if (activeTestament == 1) {
            ThemeHelper.stylePrimaryButton(this, btnTabAT);
            ThemeHelper.styleBarButton(this, btnTabNT);
        } else {
            ThemeHelper.styleBarButton(this, btnTabAT);
            ThemeHelper.stylePrimaryButton(this, btnTabNT);
        }

        visibleBooks.clear();
        for (int i = 0; i < allBooks.size(); i++) {
            BookRow b = allBooks.get(i);
            if (b.testamentId == activeTestament) {
                visibleBooks.add(b);
            }
        }

        ArrayAdapter<BookRow> adapter = new ArrayAdapter<BookRow>(
                this,
                android.R.layout.simple_list_item_1,
                visibleBooks
        );
        listBooks.setAdapter(adapter);
    }

    private void showChaptersForBook(BookRow book) {
        selectedBook = book;
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

        txtTopTitle.setText(book.name);
        testamentTabs.setVisibility(View.GONE);
        listBooks.setVisibility(View.GONE);
        gridChapters.setVisibility(View.VISIBLE);

        final int bgRes = ThemeHelper.isDarkTheme(this)
                ? R.drawable.btn_kairos_dark
                : R.drawable.btn_kairos_light;
        final int textColor = ThemeHelper.getTextColor(this);
        final int minHeightPx = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                52,
                getResources().getDisplayMetrics()
        );

        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(
                this,
                android.R.layout.simple_list_item_1,
                chapters
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv;
                if (convertView instanceof TextView) {
                    tv = (TextView) convertView;
                } else {
                    tv = new TextView(BiblePickerActivity.this);
                    tv.setGravity(Gravity.CENTER);
                    tv.setMinHeight(minHeightPx);
                    tv.setTextSize(18);
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                tv.setBackgroundResource(bgRes);
                tv.setTextColor(textColor);
                tv.setText(String.valueOf(chapters.get(position)));
                return tv;
            }
        };
        gridChapters.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bibleDb != null && bibleDb.isOpen()) {
            bibleDb.close();
        }
    }
}
