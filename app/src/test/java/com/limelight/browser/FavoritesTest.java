package com.limelight.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class FavoritesTest {
    @Test
    public void addAndListKeepsOrder() {
        Favorites f = new Favorites();
        f.add("https://a.example/", "A");
        f.add("https://b.example/", "B");
        List<Favorites.Entry> list = f.list();
        assertEquals(2, list.size());
        assertEquals("https://a.example/", list.get(0).url);
        assertEquals("B", list.get(1).title);
    }

    @Test
    public void reAddMovesToEnd() {
        Favorites f = new Favorites();
        f.add("https://a.example/", "A");
        f.add("https://b.example/", "B");
        f.add("https://a.example/", "A2");
        List<Favorites.Entry> list = f.list();
        assertEquals("https://b.example/", list.get(0).url);
        assertEquals("A2", list.get(1).title);
    }

    @Test
    public void blankUrlIgnoredTitleDefaults() {
        Favorites f = new Favorites();
        f.add("   ", "X");
        f.add(null, "X");
        f.add("https://a.example/", "  ");
        assertEquals(1, f.size());
        assertEquals("https://a.example/", f.list().get(0).title);
    }

    @Test
    public void removeAndContains() {
        Favorites f = new Favorites();
        f.add("https://a.example/", "A");
        assertTrue(f.contains("https://a.example/"));
        assertTrue(f.remove("https://a.example/"));
        assertFalse(f.contains("https://a.example/"));
        assertFalse(f.remove("https://a.example/"));
    }

    @Test
    public void storageRoundTrip() {
        Favorites f = new Favorites();
        f.add("https://a.example/x?y=1", "A & B");
        f.add("https://b.example/", "C");
        Favorites g = Favorites.fromStorage(f.toStorage());
        assertEquals(2, g.size());
        assertEquals("A & B", g.list().get(0).title);
        assertEquals("https://b.example/", g.list().get(1).url);
    }

    @Test
    public void storageEmpty() {
        assertEquals(0, Favorites.fromStorage(null).size());
        assertEquals(0, Favorites.fromStorage("").size());
        assertEquals("", new Favorites().toStorage());
    }

    @Test
    public void pageEscapesAndLinks() {
        Favorites f = new Favorites();
        f.add("https://a.example/?q=<x>", "A\"B");
        String page = f.toPage();
        assertTrue(page.contains("https://a.example/?q=&lt;x&gt;"));
        assertTrue(page.contains("A&quot;B"));
        assertTrue(page.contains(Favorites.REMOVE_PREFIX
                + "https://a.example/?q=&lt;x&gt;"));
    }

    @Test
    public void pageEmptyState() {
        assertTrue(new Favorites().toPage().contains("Sin favoritos"));
    }

    @Test
    public void escapeRoundTrip() {
        String url = "https://a.example/?q=<x>&y=\"z\"";
        assertEquals(url, Favorites.unescapeHtml(Favorites.escapeHtml(url)));
    }
}
