package com.limelight.browser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Favorites store: insertion-ordered url to title map with a line-based
 * storage encoding and a local HTML page. No Android dependencies, so
 * plain JUnit covers it.
 */
public final class Favorites {
    public static final String REMOVE_PREFIX = "fav:remove:";
    public static final String PAGE_URL = "fav://local";

    public static final class Entry {
        public final String url;
        public final String title;

        public Entry(String url, String title) {
            this.url = url;
            this.title = title;
        }
    }

    private final LinkedHashMap<String, String> items = new LinkedHashMap<>();

    public void add(String url, String title) {
        if (url == null) {
            return;
        }
        String u = url.trim();
        if (u.isEmpty()) {
            return;
        }
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) {
            t = u;
        }
        items.remove(u);
        items.put(u, singleLine(t));
    }

    public boolean remove(String url) {
        return url != null && items.remove(url.trim()) != null;
    }

    public boolean contains(String url) {
        return url != null && items.containsKey(url.trim());
    }

    public List<Entry> list() {
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<String, String> e : items.entrySet()) {
            out.add(new Entry(e.getKey(), e.getValue()));
        }
        return out;
    }

    public int size() {
        return items.size();
    }

    private static String singleLine(String s) {
        return s.replace('\n', ' ').replace('\r', ' ');
    }

    /** Storage: one "url\\ntitle" record per entry, blank line between. */
    public String toStorage() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : items.entrySet()) {
            sb.append(e.getKey()).append('\n').append(e.getValue()).append("\n\n");
        }
        return sb.toString();
    }

    public static Favorites fromStorage(String s) {
        Favorites f = new Favorites();
        if (s == null || s.isEmpty()) {
            return f;
        }
        for (String record : s.split("\n\n")) {
            int nl = record.indexOf('\n');
            if (nl > 0) {
                f.add(record.substring(0, nl), record.substring(nl + 1));
            }
        }
        return f;
    }

    public static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    public static String unescapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&quot;", "\"").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&amp;", "&");
    }

    /** Local page listing favorites with remove links. */
    public String toPage() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset=\"utf-8\">"
                + "<style>body{background:#101828;color:#fff;"
                + "font-family:sans-serif;padding:24px}"
                + "a{color:#35e0ff;font-size:20px}"
                + ".rm{color:#8892a8;font-size:16px;margin-left:16px}"
                + "li{margin:14px 0}</style></head><body>");
        sb.append("<h2>Favoritos</h2>");
        if (items.isEmpty()) {
            sb.append("<p>Sin favoritos. Abre una pagina y pulsa *.</p>");
        } else {
            sb.append("<ul>");
            for (Map.Entry<String, String> e : items.entrySet()) {
                sb.append("<li><a href=\"").append(escapeHtml(e.getKey())).append("\">")
                        .append(escapeHtml(e.getValue())).append("</a>")
                        .append("<a class=\"rm\" href=\"").append(REMOVE_PREFIX)
                        .append(escapeHtml(e.getKey())).append("\">[quitar]</a></li>");
            }
            sb.append("</ul>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}
