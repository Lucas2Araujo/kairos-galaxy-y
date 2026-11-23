package br.com.kairos.galaxyy.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import br.com.kairos.galaxyy.R;

public class ThemeHelper {

    private static final String PREFS_NAME = "kairos_lite_prefs";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_FONT_SIZE = "font_size_sp";

    public static final int FONT_MIN_SP = 13;
    public static final int FONT_MAX_SP = 24;
    public static final int FONT_DEFAULT_SP = 16;
    public static final int FONT_STEP_SP = 2;

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

    public static int getFontSize(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int size = prefs.getInt(KEY_FONT_SIZE, FONT_DEFAULT_SP);
        if (size < FONT_MIN_SP) return FONT_MIN_SP;
        if (size > FONT_MAX_SP) return FONT_MAX_SP;
        return size;
    }

    public static int adjustFontSize(Context context, int delta) {
        int current = getFontSize(context);
        int next = current + delta;
        if (next < FONT_MIN_SP) next = FONT_MIN_SP;
        if (next > FONT_MAX_SP) next = FONT_MAX_SP;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_FONT_SIZE, next).commit();
        return next;
    }

    public static int getBackgroundColor(Context context) {
        return isDarkTheme(context) ? Color.BLACK : Color.WHITE;
    }

    public static int getTopBarColor(Context context) {
        return isDarkTheme(context) ? 0xFF111111 : 0xFFF7F4EF;
    }

    public static int getDividerColor(Context context) {
        return isDarkTheme(context) ? 0xFF2B241B : 0xFFDFD3C3;
    }

    public static int getAccentColor(Context context) {
        return isDarkTheme(context) ? 0xFFD9A773 : 0xFF8C5A2B;
    }

    public static int getTextColor(Context context) {
        return isDarkTheme(context) ? 0xFFF5F5F5 : 0xFF111111;
    }

    public static int getSecondaryTextColor(Context context) {
        return isDarkTheme(context) ? 0xFFAAAAAA : 0xFF555555;
    }

    public static int getHighlightBgColor(Context context) {
        return isDarkTheme(context) ? 0xFF3A2B16 : 0xFFF5E6CC;
    }

    public static void stylePrimaryButton(Context context, Button button) {
        boolean dark = isDarkTheme(context);
        button.setBackgroundResource(dark ? R.drawable.btn_kairos_dark : R.drawable.btn_kairos_light);
        button.setTextColor(getTextColor(context));
    }

    public static void styleBarButton(Context context, Button button) {
        boolean dark = isDarkTheme(context);
        button.setBackgroundResource(dark ? R.drawable.btn_bar_dark : R.drawable.btn_bar_light);
        button.setTextColor(getAccentColor(context));
    }

    public static void styleRefCard(Context context, TextView view) {
        boolean dark = isDarkTheme(context);
        view.setBackgroundResource(dark ? R.drawable.card_ref_dark : R.drawable.card_ref_light);
        view.setTextColor(getAccentColor(context));
    }

    public static void styleTopBar(final Activity activity, View topBar, View divider, TextView titleView, Button backBtn) {
        if (topBar != null) {
            topBar.setBackgroundColor(getTopBarColor(activity));
        }
        if (divider != null) {
            divider.setBackgroundColor(getDividerColor(activity));
        }
        if (titleView != null) {
            titleView.setTextColor(getAccentColor(activity));
        }
        if (backBtn != null) {
            styleBarButton(activity, backBtn);
            backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.finish();
                }
            });
        }
    }
}
