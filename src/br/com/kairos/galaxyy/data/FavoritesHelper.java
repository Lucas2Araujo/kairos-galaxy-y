package br.com.kairos.galaxyy.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

public class FavoritesHelper {

    private static final String PREFS = "kairos_favorites";
    private static final String KEY_ACTIVE_HYMNAL = "active_hymnal";
    private static final String KEY_LAST_BOOK_ID = "last_bible_book_id";
    private static final String KEY_LAST_CHAPTER = "last_bible_chapter";

    public static Set<Integer> getFavorites(Context context, String dbName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString("favs_" + dbName, "");
        Set<Integer> result = new LinkedHashSet<Integer>();
        if (raw != null && raw.length() > 0) {
            String[] parts = raw.split(",");
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i].trim();
                if (p.length() > 0) {
                    try {
                        result.add(Integer.valueOf(Integer.parseInt(p)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return result;
    }

    public static boolean isFavorite(Context context, String dbName, int hymnId) {
        return getFavorites(context, dbName).contains(Integer.valueOf(hymnId));
    }

    public static boolean toggleFavorite(Context context, String dbName, int hymnId) {
        Set<Integer> favs = getFavorites(context, dbName);
        Integer key = Integer.valueOf(hymnId);
        boolean added;
        if (favs.contains(key)) {
            favs.remove(key);
            added = false;
        } else {
            favs.add(key);
            added = true;
        }
        StringBuilder sb = new StringBuilder();
        for (Integer id : favs) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id.intValue());
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString("favs_" + dbName, sb.toString()).commit();
        return added;
    }

    public static String getActiveHymnalDb(Context context, String defaultDb) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACTIVE_HYMNAL, defaultDb);
    }

    public static void setActiveHymnalDb(Context context, String dbName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACTIVE_HYMNAL, dbName).commit();
    }

    public static int getLastBibleBookId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_LAST_BOOK_ID, 1);
    }

    public static int getLastBibleChapter(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_LAST_CHAPTER, 1);
    }

    public static void saveLastBiblePosition(Context context, int bookId, int chapter) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_LAST_BOOK_ID, bookId)
                .putInt(KEY_LAST_CHAPTER, chapter)
                .commit();
    }
}
