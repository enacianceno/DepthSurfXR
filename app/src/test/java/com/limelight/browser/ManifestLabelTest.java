package com.limelight.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

// The Quest launcher shows the application label. It is not in the main
// manifest: the nonRoot flavor manifest points it at @string/app_label,
// which app/build.gradle generates per build type. All three must agree
// on DepthSurfXR or the app shows up as Moonlight XR.
public class ManifestLabelTest {
    private static String read(String... candidates) throws Exception {
        for (String candidate : candidates) {
            Path path = Paths.get(System.getProperty("user.dir"), candidate);
            if (Files.isRegularFile(path)) {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            }
        }
        fail("not found from " + System.getProperty("user.dir"));
        return null;
    }

    private static String mainManifest() throws Exception {
        return read("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml");
    }

    @Test
    public void launcherActivityLabelIsDepthSurfXR() throws Exception {
        assertTrue("BrowserActivity must carry android:label=\"DepthSurfXR\"",
                Pattern.compile("android:name=\"\\.browser\\.BrowserActivity\"\\s+"
                        + "android:label=\"DepthSurfXR\"").matcher(mainManifest()).find());
    }

    @Test
    public void nonRootManifestUsesAppLabel() throws Exception {
        String flavor = read("src/nonRoot/AndroidManifest.xml",
                "app/src/nonRoot/AndroidManifest.xml");
        assertTrue("nonRoot manifest must point the label at @string/app_label",
                flavor.contains("android:label=\"@string/app_label\""));
    }

    @Test
    public void gradleAppLabelIsDepthSurfXR() throws Exception {
        String gradle = read("build.gradle", "app/build.gradle");
        Matcher m = Pattern.compile(
                "resValue\\s+\"string\",\\s*\"app_label\",\\s*\"([^\"]+)\"")
                .matcher(gradle);
        int count = 0;
        while (m.find()) {
            count++;
            assertEquals("app_label resValue #" + count + " must be DepthSurfXR",
                    "DepthSurfXR", m.group(1));
        }
        assertEquals("expected debug + release app_label resValues", 2, count);
    }
}
