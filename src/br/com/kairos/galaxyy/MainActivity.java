package br.com.kairos.galaxyy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import br.com.kairos.galaxyy.ui.ThemeHelper;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.rootMain);
        TextView txtBrand = (TextView) findViewById(R.id.txtBrand);
        View brandAccentBar = findViewById(R.id.brandAccentBar);
        TextView txtSubtitle = (TextView) findViewById(R.id.txtSubtitle);
        Button btnBible = (Button) findViewById(R.id.btnOpenBible);
        Button btnHymnal = (Button) findViewById(R.id.btnOpenHymnal);
        Button btnSunset = (Button) findViewById(R.id.btnOpenSunset);
        Button btnToggleTheme = (Button) findViewById(R.id.btnToggleTheme);

        root.setBackgroundColor(ThemeHelper.getBackgroundColor(this));
        txtBrand.setTextColor(ThemeHelper.getAccentColor(this));
        brandAccentBar.setBackgroundColor(ThemeHelper.getAccentColor(this));
        txtSubtitle.setTextColor(ThemeHelper.getSecondaryTextColor(this));

        ThemeHelper.stylePrimaryButton(this, btnBible);
        ThemeHelper.stylePrimaryButton(this, btnHymnal);
        ThemeHelper.stylePrimaryButton(this, btnSunset);
        ThemeHelper.styleBarButton(this, btnToggleTheme);

        btnToggleTheme.setText(
                ThemeHelper.isDarkTheme(this)
                        ? R.string.theme_dark_label
                        : R.string.theme_light_label
        );

        btnBible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BibleActivity.class));
            }
        });

        btnHymnal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HymnalActivity.class));
            }
        });

        btnSunset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SunsetActivity.class));
            }
        });

        btnToggleTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.toggleTheme(MainActivity.this);
            }
        });
    }
}
