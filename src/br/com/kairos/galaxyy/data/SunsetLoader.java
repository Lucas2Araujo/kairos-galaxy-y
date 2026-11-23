package br.com.kairos.galaxyy.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SunsetLoader {

    public static class MeditationItem {
        public final String publishedAt;
        public final String title;
        public final String verseText;
        public final String verseReference;
        public final String content;
        public final String author;

        public MeditationItem(
                String publishedAt,
                String title,
                String verseText,
                String verseReference,
                String content,
                String author
        ) {
            this.publishedAt = publishedAt;
            this.title = title;
            this.verseText = verseText;
            this.verseReference = verseReference;
            this.content = content;
            this.author = author;
        }

        public String formatDisplayDate() {
            if (publishedAt != null && publishedAt.length() == 10 && publishedAt.charAt(4) == '-') {
                return publishedAt.substring(8, 10) + "/" + publishedAt.substring(5, 7);
            }
            return publishedAt != null ? publishedAt : "";
        }

        @Override
        public String toString() {
            String shortDate = formatDisplayDate();
            return shortDate.length() > 0 ? (shortDate + " - " + title) : title;
        }
    }

    public static List<MeditationItem> loadFromAssets(Context context, String fileName) throws Exception {
        InputStream is = context.getAssets().open(fileName);
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
        }

        List<MeditationItem> items = new ArrayList<MeditationItem>();
        JSONArray array = new JSONArray(sb.toString());
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String publishedAt = obj.optString("published_at", "");
            String title = obj.optString("title", "Meditação " + (i + 1));
            String verseText = obj.optString("verse_text", "");
            String verseRef = obj.optString("verse_reference", "");
            String content = obj.optString("content", "");
            String author = obj.optString("author", "");
            items.add(new MeditationItem(publishedAt, title, verseText, verseRef, content, author));
        }
        return items;
    }

    public static int findClosestIndex(List<MeditationItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        long nowMs = new Date().getTime();
        int bestIndex = 0;
        long bestDiff = Long.MAX_VALUE;

        for (int i = 0; i < items.size(); i++) {
            String dateStr = items.get(i).publishedAt;
            if (dateStr == null || dateStr.length() == 0) {
                continue;
            }
            try {
                Date d = fmt.parse(dateStr);
                if (d != null) {
                    long diff = Math.abs(nowMs - d.getTime());
                    if (diff < bestDiff) {
                        bestDiff = diff;
                        bestIndex = i;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return bestIndex;
    }
}
