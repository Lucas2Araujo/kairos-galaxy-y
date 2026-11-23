package br.com.kairos.galaxyy.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ThemeHelper {

    private static final String PREFS_NAME = "kairos_lite_prefs";
    private static final String KEY_DARK_THEME = "dark_theme";

    public static boolean isDarkTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DARK_THEME, true);
    }

    public static void applyTheme(Activity activity) {
        if (isDarkTheme(activity)) {
            activity.setTheme(android.R.style.Theme_Black_NoTitleBar);
        } else {
            activity.setTheme(android.R.style.Theme_Light_NoTitleBar);
        }
    }

    public static void toggleTheme(Activity activity) {
        boolean nextDark = !isDarkTheme(activity);
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_THEME, nextDark).commit();

        Intent intent = activity.getIntent();
        activity.finish();
        activity.startActivity(intent);
    }
}
