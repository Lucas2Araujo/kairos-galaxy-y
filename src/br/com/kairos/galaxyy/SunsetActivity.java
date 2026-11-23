package br.com.kairos.galaxyy;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
    private TextView txtMeditationBody;
    private List<SunsetLoader.MeditationItem> meditations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sunset);

        spinnerMeditations = (Spinner) findViewById(R.id.spinnerMeditations);
        scrollSunset = (ScrollView) findViewById(R.id.scrollSunset);
        txtMeditationBody = (TextView) findViewById(R.id.txtMeditationBody);

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

    private void renderMeditation(SunsetLoader.MeditationItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append(item.title);
        String date = item.formatDisplayDate();
        if (date.length() > 0) {
            sb.append(" (").append(date).append(")");
        }
        sb.append("\n\n");

        if (item.verseText != null && item.verseText.trim().length() > 0) {
            sb.append("\"").append(item.verseText.trim()).append("\"");
            if (item.verseReference != null && item.verseReference.trim().length() > 0) {
                sb.append("\n— ").append(item.verseReference.trim());
            }
            sb.append("\n\n");
        }

        if (item.content != null && item.content.trim().length() > 0) {
            sb.append(item.content.trim());
        }

        if (item.author != null && item.author.trim().length() > 0) {
            sb.append("\n\nAutor: ").append(item.author.trim());
        }

        txtMeditationBody.setText(sb.toString());
        scrollSunset.post(new Runnable() {
            @Override
            public void run() {
                scrollSunset.scrollTo(0, 0);
            }
        });
    }
}
