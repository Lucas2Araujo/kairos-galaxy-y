package br.com.kairos.galaxyy;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import br.com.kairos.galaxyy.data.SunsetLoader;
import br.com.kairos.galaxyy.ui.ThemeHelper;

public class SunsetActivity extends Activity {

    private Spinner spinnerMeditations;
    private ScrollView scrollSunset;
    private TextView txtSunsetBibleRefCard;
    private TextView txtMeditationBody;
    private List<SunsetLoader.MeditationItem> meditations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_sunset);

        View root = findViewById(R.id.rootSunset);
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
        ThemeHelper.styleBarButton(this, btnFontDown);
        ThemeHelper.styleBarButton(this, btnFontUp);

        spinnerMeditations = (Spinner) findViewById(R.id.spinnerMeditations);
        scrollSunset = (ScrollView) findViewById(R.id.scrollSunset);
        txtSunsetBibleRefCard = (TextView) findViewById(R.id.txtSunsetBibleRefCard);
        txtMeditationBody = (TextView) findViewById(R.id.txtMeditationBody);

        ThemeHelper.styleRefCard(this, txtSunsetBibleRefCard);
        txtMeditationBody.setTextColor(ThemeHelper.getTextColor(this));
        applyFontSize();

        btnFontDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.adjustFontSize(SunsetActivity.this, -ThemeHelper.FONT_STEP_SP);
                applyFontSize();
            }
        });

        btnFontUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.adjustFontSize(SunsetActivity.this, ThemeHelper.FONT_STEP_SP);
                applyFontSize();
            }
        });

        try {
            meditations = SunsetLoader.loadFromAssets(this, "meditacoes_2026.json");
            ArrayAdapter<SunsetLoader.MeditationItem> adapter = new ArrayAdapter<SunsetLoader.MeditationItem>(
                    this,
                    android.R.layout.simple_spinner_item,
                    meditations
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerMeditations.setAdapter(adapter);

            spinnerMeditations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < meditations.size()) {
                        renderMeditation(meditations.get(position));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            int closest = SunsetLoader.findClosestIndex(meditations);
            spinnerMeditations.setSelection(closest);
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    getString(R.string.error_open_sunset) + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void applyFontSize() {
        txtMeditationBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, ThemeHelper.getFontSize(this));
    }

    private void renderMeditation(SunsetLoader.MeditationItem item) {
        if (item.verseReference != null && item.verseReference.trim().length() > 0) {
            final String cleanRef = item.verseReference.trim();
            txtSunsetBibleRefCard.setText(getString(R.string.open_in_bible_prefix) + cleanRef + " >");
            txtSunsetBibleRefCard.setVisibility(View.VISIBLE);
            txtSunsetBibleRefCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SunsetActivity.this, BibleActivity.class);
                    intent.putExtra(BibleActivity.EXTRA_BIBLE_REF, cleanRef);
                    startActivity(intent);
                }
            });
        } else {
            txtSunsetBibleRefCard.setVisibility(View.GONE);
        }

        int accentColor = ThemeHelper.getAccentColor(this);
        int secondaryColor = ThemeHelper.getSecondaryTextColor(this);

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        int titleStart = ssb.length();
        ssb.append(item.title);
        int titleEnd = ssb.length();
        ssb.setSpan(new StyleSpan(Typeface.BOLD), titleStart, titleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new ForegroundColorSpan(accentColor), titleStart, titleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new RelativeSizeSpan(1.15f), titleStart, titleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        String date = item.formatDisplayDate();
        if (date.length() > 0) {
            ssb.append("  ");
            int dateStart = ssb.length();
            ssb.append(date);
            int dateEnd = ssb.length();
            ssb.setSpan(new ForegroundColorSpan(secondaryColor), dateStart, dateEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new RelativeSizeSpan(0.85f), dateStart, dateEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ssb.append("\n\n");

        if (item.verseText != null && item.verseText.trim().length() > 0) {
            int vStart = ssb.length();
            ssb.append("\"").append(item.verseText.trim()).append("\"");
            int vEnd = ssb.length();
            ssb.setSpan(new StyleSpan(Typeface.ITALIC), vStart, vEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (item.verseReference != null && item.verseReference.trim().length() > 0) {
                ssb.append("\n");
                int rStart = ssb.length();
                ssb.append("— ").append(item.verseReference.trim());
                int rEnd = ssb.length();
                ssb.setSpan(new StyleSpan(Typeface.BOLD), rStart, rEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(accentColor), rStart, rEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new RelativeSizeSpan(0.9f), rStart, rEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            ssb.append("\n\n");
        }

        if (item.content != null && item.content.trim().length() > 0) {
            ssb.append(item.content.trim());
        }

        if (item.author != null && item.author.trim().length() > 0) {
            ssb.append("\n\n");
            int aStart = ssb.length();
            ssb.append("Autor: ").append(item.author.trim());
            int aEnd = ssb.length();
            ssb.setSpan(new ForegroundColorSpan(secondaryColor), aStart, aEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new RelativeSizeSpan(0.85f), aStart, aEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        txtMeditationBody.setText(ssb);
        scrollSunset.post(new Runnable() {
            @Override
            public void run() {
                scrollSunset.scrollTo(0, 0);
            }
        });
    }
}
