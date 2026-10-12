package com.limelight.browser;

import android.app.Activity;
import android.app.Presentation;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limelight.LimeLog;
import com.limelight.binding.video.XrRenderer;
import com.limelight.browser.WebViewInputMapper.TouchCmd;
import com.limelight.browser.WebViewInputMapper.TouchKind;
import com.limelight.binding.input.VrKeyboard;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * Etapa B: local WebView as Moonlight's content source. No streaming.
 *
 * <p>Starts an unmodified {@link XrRenderer} and hands its decoder-facing
 * Surface to a WebView VirtualDisplay (GPU to GPU, zero copies), then routes
 * {@link XrRenderer.InputListener} callbacks into the page. Keys are logged
 * until Etapa C wires them to focused fields.
 */
public class BrowserActivity extends Activity implements XrRenderer.InputListener {
    private static final String HOME_URL = "https://www.google.com/";
    private PreferenceConfiguration prefs;
    private int webW = Aspect.LAND_W;
    private int webH = Aspect.LAND_H;
    private float lastAspectSeen = -1.0f;
    private float lastAspectApplied = -1.0f;
    private long lastAspectApplyMs;
    private boolean aspectTrailingPosted;
    // 60% of the original 320 dpi: page content renders smaller in the window.
    private static final int WEB_DPI = 192;

    private XrRenderer xrRenderer;
    private WebView webView;
    private EditText urlField;
    private ViewGroup rootView;
    private LinearLayout toolbar;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private Button favButton;
    private Button restoreButton;
    private Button incognitoButton;
    private String stockUa;
    private String desktopUaString;
    private String uaFamily = UaPolicy.DESKTOP;
    private String uaReloadUrl;
    private ViewGroup frameRoot;
    private LinearLayout submenuBar;
    private boolean incognito;
    private final java.util.List<String> historyUrls = new java.util.ArrayList<>();
    private Favorites favorites;
    private VirtualDisplay virtualDisplay;
    private Presentation presentation;
    private Handler uiHandler;
    private float lastU = 0.5f;
    private float lastV = 0.5f;
    private boolean buttonDown;
    private long downTimeMs;
    private final Isolation isolation = new Isolation();
    private VrKeyboard vrKeyboard;
    private int heldMeta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        uiHandler = new Handler(Looper.getMainLooper());
        favorites = Favorites.fromStorage(
                getPreferences(MODE_PRIVATE).getString("favs", ""));
        String hist = getPreferences(MODE_PRIVATE).getString("hist", "");
        if (hist != null && !hist.isEmpty()) {
            for (String u : hist.split("\n")) {
                if (!u.isEmpty()) historyUrls.add(u);
            }
        }

        prefs = PreferenceConfiguration.readPreferences(this);
        webW = Aspect.LAND_W;
        webH = Aspect.LAND_H;
        // A free corner-drag shape survives reboots; the preset is only the
        // fallback for a first run.
        float live = getPreferences(MODE_PRIVATE).getFloat("aspect_live", -1.0f);
        if (live >= Aspect.MIN_ASPECT && live <= Aspect.MAX_ASPECT) {
            int[] px = Aspect.pixelsFor(live);
            webW = px[0];
            webH = px[1];
        }
        vrKeyboard = new VrKeyboard(new WebSink());
        requestPermissions(new String[]{android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO}, 7301);
        if (!startXrSession(webW, webH)) {
            fatal("XR session failed to start");
            return;
        }

        DisplayManager displays = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        virtualDisplay = displays.createVirtualDisplay("DepthSurfBrowser",
                webW, webH, WEB_DPI, xrRenderer.getInputSurface(), 0);
        if (virtualDisplay == null) {
            fatal("VirtualDisplay failed");
            return;
        }
        presentation = new Presentation(this, virtualDisplay.getDisplay());
        Context ctx = presentation.getContext();
        float density = ctx.getResources().getDisplayMetrics().density;

        rootView = new LinearLayout(ctx);
        ((LinearLayout) rootView).setOrientation(LinearLayout.VERTICAL);
        rootView.setBackgroundColor(0xFF101828);

        toolbar = new LinearLayout(ctx);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(0xFF171B22);
        int barPx = Math.round(56 * density);
        int padPx = Math.round(6 * density);
        toolbar.setPadding(padPx, padPx, padPx, padPx);

        Button back = chromeButton(ctx, "\u2190", density);
        back.setOnClickListener(v -> {
            if (customView != null) {
                exitFullscreen();
            } else if (webView.canGoBack()) {
                webView.goBack();
            }
        });
        Button fwd = chromeButton(ctx, "\u2192", density);
        fwd.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });
        Button reload = chromeButton(ctx, "\u21BB", density);
        reload.setOnClickListener(v -> webView.reload());
        Button home = chromeButton(ctx, "\u2302", density);
        home.setOnClickListener(v -> {
            webView.loadUrl(HOME_URL);
            webView.requestFocus();
        });

        urlField = new EditText(ctx);
        urlField.setSingleLine(true);
        urlField.setTextSize(16);
        urlField.setTextColor(0xFFFFFFFF);
        urlField.setHintTextColor(0xFF8892A8);
        urlField.setHint("URL o busqueda");
        android.graphics.drawable.GradientDrawable urlBg =
                new android.graphics.drawable.GradientDrawable();
        urlBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        urlBg.setCornerRadius(22 * density);
        urlBg.setColor(0xFF232830);
        urlField.setBackground(urlBg);
        urlField.setPadding(Math.round(16 * density), 0,
                Math.round(16 * density), 0);
        urlField.setText(HOME_URL);
        urlField.setShowSoftInputOnFocus(false);
        urlField.setOnKeyListener((v, code, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && (code == KeyEvent.KEYCODE_ENTER
                            || code == KeyEvent.KEYCODE_DPAD_CENTER)) {
                goFromBar();
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        urlLp.leftMargin = padPx;
        urlLp.rightMargin = padPx;

        favButton = chromeButton(ctx, "\u2605", density);
        favButton.setOnClickListener(v -> saveFavorite());
        Button menuBtn = chromeButton(ctx, "\u22EE", density);
        menuBtn.setOnClickListener(v -> showOverflowMenu(menuBtn));
        Button maxBtn = chromeButton(ctx, "\u26F6", density);
        maxBtn.setOnClickListener(v -> setMaximized(true));

        toolbar.addView(back);
        toolbar.addView(reload);
        toolbar.addView(fwd);
        toolbar.addView(urlField, urlLp);
        toolbar.addView(favButton);
        toolbar.addView(home);
        toolbar.addView(menuBtn);
        toolbar.addView(maxBtn);
        rootView.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, barPx));

        submenuBar = new LinearLayout(ctx);
        submenuBar.setOrientation(LinearLayout.HORIZONTAL);
        submenuBar.setBackgroundColor(0xFF171B22);
        submenuBar.setPadding(padPx, padPx, padPx, padPx);
        submenuBar.setVisibility(View.GONE);
        submenuBar.addView(pillButton(ctx, "Favoritos", density,
                v -> showFavorites()));
        submenuBar.addView(pillButton(ctx, "Historial", density,
                v -> showHistory()));
        incognitoButton = pillButton(ctx, "Incognito", density,
                v -> setIncognito(!incognito));
        submenuBar.addView(incognitoButton);
        submenuBar.addView(pillButton(ctx, "Pegar e ir", density,
                v -> pasteAndGo()));
        rootView.addView(submenuBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        webView = new WebView(ctx);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        // Large sites restrict the stock "; wv" WebView token (TikTok serves
        // a degraded wall to it): present as plain Chrome instead.
        stockUa = BrowserUa.stripWebViewToken(
                webView.getSettings().getUserAgentString());
        desktopUaString = BrowserUa.desktopChrome(
                webView.getSettings().getUserAgentString());
        webView.getSettings().setUserAgentString(desktopUaString);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url,
                                      android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                String fam = UaPolicy.familyFor(url);
                if (!fam.equals(uaFamily)) {
                    uaFamily = fam;
                    view.getSettings().setUserAgentString(
                            UaPolicy.GOOGLE.equals(fam) ? stockUa : desktopUaString);
                    LimeLog.info("BrowserDiag ua family=" + fam);
                    if (url != null && !url.equals(uaReloadUrl)) {
                        uaReloadUrl = url;
                        view.reload();
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                recordHistory(url);
                if (urlField != null && !urlField.hasFocus() && url != null
                        && !url.startsWith("fav:") && !url.startsWith("about:")) {
                    urlField.setText(url);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return request != null && request.getUrl() != null
                        && handleLink(request.getUrl().toString());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && error != null) {
                    LimeLog.warning(WebEngineDiagnostics.logLine(
                            request.isForMainFrame()
                                    ? WebEngineDiagnostics.Scope.MAIN_FRAME
                                    : WebEngineDiagnostics.Scope.SUBRESOURCE,
                            String.valueOf(request.getUrl()), null,
                            error.getErrorCode(),
                            String.valueOf(error.getDescription()), false));
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (request != null && errorResponse != null) {
                    LimeLog.warning(WebEngineDiagnostics.logLine(
                            request.isForMainFrame()
                                    ? WebEngineDiagnostics.Scope.MAIN_FRAME
                                    : WebEngineDiagnostics.Scope.SUBRESOURCE,
                            String.valueOf(request.getUrl()),
                            errorResponse.getStatusCode(), null,
                            errorResponse.getReasonPhrase(), false));
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleLink(url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(
                    android.webkit.PermissionRequest request) {
                java.util.List<String> granted = new java.util.ArrayList<>();
                for (String r : request.getResources()) {
                    boolean ok = false;
                    if (android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) {
                        ok = checkSelfPermission(android.Manifest.permission.CAMERA)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED;
                    } else if (android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) {
                        ok = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED;
                    }
                    LimeLog.info("BrowserDiag perm resource=" + r + " granted=" + ok);
                    if (ok) granted.add(r);
                }
                if (granted.isEmpty()) request.deny();
                else request.grant(granted.toArray(new String[0]));
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customCallback = callback;
                toolbar.setVisibility(View.GONE);
                rootView.addView(view, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
            }

            @Override
            public void onHideCustomView() {
                exitFullscreen();
            }

            @Override
            public void onCloseWindow(WebView view) {
                // OAuth popups close themselves on completion; the opener
                // (main view) must never be touched here.
                if (view != null && view != webView) {
                    view.destroy();
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                if (message != null) {
                    LimeLog.warning("BrowserDiag console level=" + message.messageLevel() + " msg="
                            + String.valueOf(message.message()).replaceAll("[\\r\\n]+", " ")
                            + " @" + WebEngineDiagnostics.sanitizeUrl(message.sourceId())
                            + ":" + message.lineNumber());
                }
                return true;
            }

            // Pages that alert() with no handler hang waiting: there is no
            // dialog surface in XR, so alerts confirm, confirms accept, and
            // prompts cancel, all logged. Unblocks login/captcha flows that
            // gate on a dialog.
            @Override
            public boolean onJsAlert(WebView view, String url, String message,
                                     JsResult result) {
                LimeLog.info("Browser: alert " + message);
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message,
                                       JsResult result) {
                LimeLog.info("Browser: confirm " + message);
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message,
                                      String defaultValue, JsPromptResult result) {
                LimeLog.info("Browser: prompt " + message);
                result.cancel();
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                // OAuth/login popups (Google "continue as") have no surface
                // in XR, but they must live: the opener page waits on them
                // via window.opener or a redirect back. The popup runs its
                // own flow invisibly with the opener intact; only a
                // navigation back to the opener's site (the completed
                // login) moves to the main view.
                final WebView popup = new WebView(view.getContext());
                popup.getSettings().setJavaScriptEnabled(true);
                popup.getSettings().setDomStorageEnabled(true);
                popup.getSettings().setUserAgentString(BrowserUa.stripWebViewToken(
                        popup.getSettings().getUserAgentString()));
                popup.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                popup.getSettings().setSupportMultipleWindows(true);
                CookieManager.getInstance().setAcceptThirdPartyCookies(popup, true);
                popup.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v,
                                                            WebResourceRequest request) {
                        if (request != null && request.getUrl() != null
                                && BrowserNav.isReturnToOpener(
                                        request.getUrl().toString(), webView.getUrl())) {
                            webView.loadUrl(request.getUrl().toString());
                            v.post(v::destroy);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, String url) {
                        if (url != null
                                && BrowserNav.isReturnToOpener(url, webView.getUrl())) {
                            webView.loadUrl(url);
                            v.post(v::destroy);
                            return true;
                        }
                        return false;
                    }
                });
                popup.setWebChromeClient(this);
                WebView.WebViewTransport transport =
                        (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }
        });
        rootView.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        frameRoot = new android.widget.FrameLayout(ctx);
        frameRoot.addView(rootView, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        restoreButton = chromeButton(ctx, "\u2715", density);
        restoreButton.setAlpha(0.75f);
        restoreButton.setVisibility(View.GONE);
        restoreButton.setOnClickListener(v -> setMaximized(false));
        android.widget.FrameLayout.LayoutParams restoreLp =
                new android.widget.FrameLayout.LayoutParams(
                        Math.round(44 * density), Math.round(44 * density),
                        android.view.Gravity.TOP | android.view.Gravity.END);
        restoreLp.topMargin = Math.round(8 * density);
        restoreLp.rightMargin = Math.round(8 * density);
        frameRoot.addView(restoreButton, restoreLp);
        presentation.setContentView(frameRoot,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        presentation.show();
        try {
            android.content.pm.PackageInfo pkg = WebView.getCurrentWebViewPackage();
            LimeLog.info("BrowserDiag provider=" + (pkg == null ? "unknown"
                    : pkg.packageName + " " + pkg.versionName)
                    + " ua=" + webView.getSettings().getUserAgentString());
        } catch (Exception e) {
            LimeLog.warning("BrowserDiag provider=unavailable");
        }
        String startUrl = HOME_URL;
        String extra = getIntent() == null ? null : getIntent().getStringExtra("dsurl");
        android.net.Uri data = getIntent() == null ? null : getIntent().getData();
        if (extra != null && extra.startsWith("http")) startUrl = extra;
        else if (data != null && String.valueOf(data).startsWith("http")) startUrl = String.valueOf(data);
        urlField.setText(startUrl);
        if ("rectest".equals(extra)) {
            uiHandler.postDelayed(() -> new StreamRecorder(rootView).startAsync(
                    new java.io.File(getExternalFilesDir(null), "rectest.mp4")), 4000);
            webView.loadUrl(startUrl);
        } else if ("gumtest".equals(extra)) {
            webView.loadDataWithBaseURL("https://example.com/",
                    "<html><body><script>"
                    + "console.log('GUMTEST start');"
                    + "navigator.mediaDevices.enumerateDevices().then(function(ds){"
                    + "console.log('GUMTEST devices='+ds.map(function(d){return d.kind;}).join(','));"
                    + "return navigator.mediaDevices.getUserMedia({audio:true});})"
                    + ".then(function(s){console.log('GUMTEST AUDIO OK');s.getTracks().forEach(function(t){t.stop();});"
                    + "return navigator.mediaDevices.getUserMedia({video:true});})"
                    + ".then(function(s){console.log('GUMTEST VIDEO OK tracks='+s.getTracks().map(function(t){return t.kind+':'+t.readyState;}).join(','));})"
                    + ".catch(function(e){console.log('GUMTEST FAIL '+e.name+': '+e.message);});"
                    + "</script></body></html>", "text/html", "UTF-8", null);
        } else {
            webView.loadUrl(startUrl);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String extra = intent == null ? null : intent.getStringExtra("dsurl");
        android.net.Uri data = intent == null ? null : intent.getData();
        String target = extra != null ? extra
                : (data != null ? String.valueOf(data) : null);
        if (target != null && target.startsWith("http") && webView != null) {
            urlField.setText(target);
            webView.loadUrl(target);
        }
    }

    private Button chromeButton(Context ctx, String glyph, float density) {
        Button b = new Button(ctx);
        b.setText(glyph);
        b.setTextSize(19);
        b.setTextColor(0xFFF2F4F7);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        android.graphics.drawable.GradientDrawable d =
                new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        d.setColor(0xFF2A3038);
        b.setBackground(d);
        int s = Math.round(44 * density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.leftMargin = Math.round(3 * density);
        lp.rightMargin = Math.round(3 * density);
        b.setLayoutParams(lp);
        return b;
    }

    private void setMaximized(boolean max) {
        if (toolbar != null) {
            toolbar.setVisibility(max ? View.GONE : View.VISIBLE);
        }
        if (submenuBar != null && max) {
            submenuBar.setVisibility(View.GONE);
        }
        if (restoreButton != null) {
            restoreButton.setVisibility(max ? View.VISIBLE : View.GONE);
        }
        // The WebView does not always repaint after the resize this
        // causes on the VirtualDisplay: force a redraw, twice.
        if (uiHandler != null) {
            uiHandler.postDelayed(() -> {
                if (rootView != null) rootView.requestLayout();
                if (webView != null) {
                    webView.invalidate();
                    webView.requestFocus();
                }
            }, 150);
        }
    }

    // In-layout panel, NOT a PopupMenu: popup windows live outside the
    // Presentation hierarchy and never receive the XR ray clicks.
    private void showOverflowMenu(View anchor) {
        if (submenuBar == null) return;
        submenuBar.setVisibility(submenuBar.getVisibility() == View.VISIBLE
                ? View.GONE : View.VISIBLE);
    }

    private Button pillButton(Context ctx, String label, float density,
                              View.OnClickListener action) {
        Button b = new Button(ctx);
        b.setText(label);
        b.setTextSize(15);
        b.setTextColor(0xFFF2F4F7);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        android.graphics.drawable.GradientDrawable d =
                new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        d.setCornerRadius(18 * density);
        d.setColor(0xFF2A3038);
        b.setBackground(d);
        int pad = Math.round(14 * density);
        b.setPadding(pad, Math.round(8 * density), pad, Math.round(8 * density));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Math.round(4 * density);
        lp.rightMargin = Math.round(4 * density);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> {
            submenuBar.setVisibility(View.GONE);
            action.onClick(v);
        });
        return b;
    }

    private void setIncognito(boolean on) {
        incognito = on;
        if (incognitoButton != null) {
            incognitoButton.setText(on ? "Salir de incognito" : "Incognito");
        }
        LimeLog.info("BrowserDiag incognito=" + on);
        if (on) {
            CookieManager.getInstance().removeAllCookies(null);
            if (webView != null) {
                webView.clearCache(true);
                webView.clearHistory();
                webView.loadUrl("about:blank");
            }
        }
    }

    private void recordHistory(String url) {
        if (incognito || url == null || !url.startsWith("http")) return;
        if (historyUrls.isEmpty() || !historyUrls.get(historyUrls.size() - 1).equals(url)) {
            historyUrls.add(url);
            while (historyUrls.size() > 50) historyUrls.remove(0);
            StringBuilder sb = new StringBuilder();
            for (String u : historyUrls) sb.append(u).append("\n");
            getPreferences(MODE_PRIVATE).edit()
                    .putString("hist", sb.toString()).apply();
        }
    }

    private void showHistory() {
        if (webView == null) return;
        StringBuilder sb = new StringBuilder(
                "<html><head><meta charset=\"utf-8\"><style>"
                + "body{background:#101828;color:#fff;font-family:sans-serif;padding:24px}"
                + "a{color:#35e0ff;font-size:20px}li{margin:14px 0}</style>"
                + "</head><body><h2>Historial</h2>");
        if (historyUrls.isEmpty()) {
            sb.append("<p>Sin historial.</p>");
        } else {
            sb.append("<ul>");
            for (int i = historyUrls.size() - 1; i >= 0; i--) {
                String u = historyUrls.get(i);
                sb.append("<li><a href=\"").append(Favorites.escapeHtml(u))
                        .append("\">").append(Favorites.escapeHtml(u))
                        .append("</a></li>");
            }
            sb.append("</ul>");
        }
        sb.append("</body></html>");
        webView.loadDataWithBaseURL("hist://local", sb.toString(),
                "text/html", "UTF-8", null);
    }

    private static Button navButton(Context ctx, String label) {
        Button b = new Button(ctx);
        b.setText(label);
        b.setTextSize(16);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        return b;
    }

    private void goFromBar() {
        if (webView == null || urlField == null) {
            return;
        }
        String raw = urlField.getText().toString().trim();
        String target = BrowserNav.normalizeUrl(raw);
        if (target != null) {
            webView.loadUrl(target);
            webView.requestFocus();
        }
    }

    /** Intercepts fav: actions tapped in the favorites page. */
    private boolean handleLink(String url) {
        if (url == null) {
            return false;
        }
        if (BrowserNav.isAppInstallLink(url,
                webView == null ? null : webView.getUrl())) {
            LimeLog.info("BrowserDiag blocked app-install link from "
                    + WebEngineDiagnostics.sanitizeUrl(
                            webView == null ? null : webView.getUrl()));
            return true;
        }
        if (BrowserNav.isExternalScheme(url)) {
            // App links (intent:, tiktok:, market:) would die as an error
            // page inside the view: hand them to Android, quietly swallow
            // when no app handles them.
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                LimeLog.warning("Browser: no app for " + url);
            }
            return true;
        }
        if (url.startsWith(Favorites.REMOVE_PREFIX)) {
            favorites.remove(Favorites.unescapeHtml(
                    url.substring(Favorites.REMOVE_PREFIX.length())));
            persistFavorites();
            showFavorites();
            return true;
        }
        return false;
    }

    /** Paste clipboard into the bar and go (XR has no long-press paste). */
    private void pasteAndGo() {
        if (urlField == null) {
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm == null) {
            return;
        }
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0
                || clip.getItemAt(0).getText() == null) {
            return;
        }
        urlField.setText(clip.getItemAt(0).getText().toString());
        goFromBar();
    }

    private void persistFavorites() {
        getPreferences(MODE_PRIVATE).edit()
                .putString("favs", favorites.toStorage()).apply();
    }

    private void saveFavorite() {
        if (webView == null || favButton == null) {
            return;
        }
        String url = webView.getUrl();
        if (url == null || url.startsWith("fav:") || url.startsWith("about:")) {
            return;
        }
        favorites.add(url, webView.getTitle());
        persistFavorites();
        favButton.setText("*+");
        uiHandler.postDelayed(() -> {
            if (favButton != null) {
                favButton.setText("*");
            }
        }, 1200);
    }

    private void showFavorites() {
        if (webView == null) {
            return;
        }
        webView.loadDataWithBaseURL(Favorites.PAGE_URL, favorites.toPage(),
                "text/html", "utf-8", null);
        webView.requestFocus();
    }

    /** Fullscreen video out: drop the custom view, bring the bar back. */
    private void exitFullscreen() {
        if (customView == null) {
            return;
        }
        if (rootView != null) {
            rootView.removeView(customView);
        }
        customView = null;
        if (toolbar != null) {
            toolbar.setVisibility(View.VISIBLE);
        }
        if (customCallback != null) {
            customCallback.onCustomViewHidden();
            customCallback = null;
        }
    }

    private void fatal(String message) {
        LimeLog.severe("Browser: " + message);
        TextView view = new TextView(this);
        view.setText(message);
        setContentView(view);
    }

    private void post(Runnable r) {
        uiHandler.post(r);
    }

    // ---- XrRenderer.InputListener (frame-loop thread; hop to UI) ----

    @Override
    public void onVrPointerMove(float u, float v) {
        post(() -> pointer(u, v));
    }

    @Override
    public void onVrMouseMove(int dx, int dy) {
        // Head-aim relative mode: nothing for a touch addressed page.
    }

    @Override
    public void onVrButton(int button, boolean down) {
        post(() -> button(button, down));
    }

    @Override
    public void onVrScroll(int clicks) {
        post(() -> scroll(clicks));
    }

    @Override
    public void onVrIsolation(boolean on) {
        post(() -> setIsolation(on));
    }

    @Override
    public void onVrAspectSize(float aspectValue) {
        post(() -> resizeDisplay(aspectValue));
    }

    // Live corner-drag reshape, throttled: the display rebinds at most
    // every 120 ms mid-drag, with one trailing pass 150 ms later so the
    // final shape always lands (changes arrive only while the value moves).
    // The page reflows to the new shape while 3D, glow and corner radius
    // follow from the same geometry, untouched.
    private void resizeDisplay(float aspectValue) {
        lastAspectSeen = aspectValue;
        if (virtualDisplay == null
                || aspectValue < Aspect.MIN_ASPECT || aspectValue > Aspect.MAX_ASPECT) {
            return;
        }
        if (SystemClock.uptimeMillis() - lastAspectApplyMs < 120) {
            if (!aspectTrailingPosted) {
                aspectTrailingPosted = true;
                uiHandler.postDelayed(() -> {
                    aspectTrailingPosted = false;
                    if (lastAspectSeen != lastAspectApplied) {
                        resizeDisplay(lastAspectSeen);
                    }
                }, 150);
            }
            return;
        }
        lastAspectApplied = aspectValue;
        lastAspectApplyMs = SystemClock.uptimeMillis();
        int[] px = Aspect.pixelsFor(aspectValue);
        if (px[0] == webW && px[1] == webH) {
            return;
        }
        webW = px[0];
        webH = px[1];
        saveLiveAspect(aspectValue);
        try {
            virtualDisplay.resize(webW, webH, WEB_DPI);
            if (xrRenderer != null) {
                xrRenderer.resizeInput(webW, webH);
            }
        } catch (Exception e) {
            LimeLog.severe("Browser: display resize: " + e);
            return;
        }
        if (rootView != null) {
            rootView.requestLayout();
        }
        if (webView != null) {
            webView.requestLayout();
        }
    }

    private void saveLiveAspect(float aspectValue) {
        getPreferences(MODE_PRIVATE).edit().putFloat("aspect_live", aspectValue).apply();
    }

    // One XR session at the given dims. First boot and aspect restarts
    // share it; only the VirtualDisplay binding differs.
    private boolean startXrSession(int w, int h) {
        XrRenderer fresh = new XrRenderer();
        fresh.setInputListener(this);
        if (!fresh.start(this, w, h, prefs)) {
            return false;
        }
        if (fresh.getInputSurface() == null || !fresh.getInputSurface().isValid()) {
            fresh.cleanup();
            return false;
        }
        xrRenderer = fresh;
        return true;
    }

    @Override
    public void onVrGamepadPlugged(boolean plugged) {
        // No gamepad in a browser.
    }

    private volatile int stickSteps;
    private boolean stickRepeating;
    private boolean stickLogged;
    private final Runnable stickRepeat = new Runnable() {
        @Override public void run() {
            int s = stickSteps;
            if (s == 0 || webView == null) {
                stickRepeating = false;
                return;
            }
            wheelAtPointer(s, s * 90);
            uiHandler.postDelayed(this, 90);
        }
    };

    private void wheelAtPointer(int steps, int fallbackPx) {
        if (webView == null || rootView == null) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[1];
        props[0] = new MotionEvent.PointerProperties();
        props[0].id = 0;
        props[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[1];
        coords[0] = new MotionEvent.PointerCoords();
        coords[0].x = lastU * viewW();
        coords[0].y = lastV * viewH();
        coords[0].setAxisValue(MotionEvent.AXIS_VSCROLL, steps);
        MotionEvent event = MotionEvent.obtain(downTimeMs, now,
                MotionEvent.ACTION_SCROLL, 1, props, coords, 0, 0, 1f, 1f,
                0, 0, android.view.InputDevice.SOURCE_MOUSE, 0);
        boolean handled = rootView.dispatchGenericMotionEvent(event);
        event.recycle();
        if (!stickLogged) {
            stickLogged = true;
            LimeLog.info("BrowserDiag stick wheel steps=" + steps + " handled=" + handled);
        }
        if (!handled) {
            webView.scrollBy(0, fallbackPx);
        }
    }

    @Override
    public void onVrGamepadState(int buttons, int leftTrigger, int rightTrigger,
                                 int leftX, int leftY, int rightX, int rightY) {
        stickSteps = StickScroll.wheelSteps(rightY != 0 ? rightY : leftY);
        if (stickSteps != 0 && uiHandler != null) {
            post(() -> {
                if (!stickRepeating) {
                    stickRepeating = true;
                    uiHandler.post(stickRepeat);
                }
            });
        }
    }

    @Override
    public void onVrKey(int code, int modifiers) {
        post(() -> vrKeyboard.type(code, modifiers));
    }

    @Override
    public void onVrModifiers(int modifiers) {
        post(() -> vrKeyboard.hold(modifiers));
    }

    @Override
    public void onVrExit() {
        post(this::finish);
    }

    private int viewW() {
        return rootView != null && rootView.getWidth() > 0 ? rootView.getWidth() : webW;
    }

    private int viewH() {
        return rootView != null && rootView.getHeight() > 0 ? rootView.getHeight() : webH;
    }

    private void pointer(float u, float v) {
        if (isolation.blocksContent()) {
            return;
        }
        lastU = u;
        lastV = v;
        if (buttonDown && rootView != null) {
            TouchCmd cmd = WebViewInputMapper.touch(TouchKind.MOVE, u, v, viewW(), viewH());
            dispatch(MotionEvent.ACTION_MOVE, cmd);
        }
    }

    private void button(int button, boolean down) {
        if (isolation.blocksContent()) {
            return;
        }
        if (button != 0 || rootView == null) {
            return;
        }
        if (down && !buttonDown) {
            buttonDown = true;
            downTimeMs = SystemClock.uptimeMillis();
            TouchCmd cmd = WebViewInputMapper.touch(TouchKind.DOWN, lastU, lastV, viewW(), viewH());
            dispatch(MotionEvent.ACTION_DOWN, cmd);
        } else if (!down && buttonDown) {
            buttonDown = false;
            TouchCmd cmd = WebViewInputMapper.touch(TouchKind.UP, lastU, lastV, viewW(), viewH());
            dispatch(MotionEvent.ACTION_UP, cmd);
        }
    }

    private void scroll(int clicks) {
        if (isolation.blocksContent()) {
            return;
        }
        if (webView != null) {
            // Native already winds the thumbstick into these clicks.
            // Feed pages (TikTok) only answer real wheel events, not
            // scrollBy, so dispatch a wheel at the ray point first.
            int clamped = Math.max(-8, Math.min(8, clicks));
            wheelAtPointer(clamped, WebViewInputMapper.scrollPixels(clicks));
        }
    }

    // The bar's content lock. Engaging it mid-drag releases the press
    // first, so the page never keeps a stuck finger. Typing stays exempt:
    // onVrKey never consults the lock.
    private void setIsolation(boolean on) {
        isolation.setOn(on);
        if (on && buttonDown && rootView != null) {
            buttonDown = false;
            TouchCmd cmd = WebViewInputMapper.touch(TouchKind.UP, lastU, lastV, viewW(), viewH());
            dispatch(MotionEvent.ACTION_UP, cmd);
        }
    }

    private void dispatch(int action, TouchCmd cmd) {
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(downTimeMs, now, action, cmd.x, cmd.y, 0);
        // frameRoot, not rootView: the floating restore button lives
        // there and must receive ray clicks while maximized.
        ViewGroup target = frameRoot != null ? frameRoot : rootView;
        target.dispatchTouchEvent(event);
        event.recycle();
    }

    /** Key target: the focused view (URL bar or page), else the WebView. */
    private View keyTarget() {
        if (rootView != null) {
            View focused = rootView.findFocus();
            if (focused != null) {
                return focused;
            }
        }
        if (webView != null) {
            webView.requestFocus();
        }
        return webView;
    }

    /** VrKeyboard sink: Moonlight's 3D keyboard into the focused field. */
    private final class WebSink implements VrKeyboard.Sink {
        @Override
        public void key(short keyCode, byte action, byte modifiers) {
            View target = keyTarget();
            if (target == null) {
                return;
            }
            int vk = keyCode & 0xFFFF;
            if (WebViewKeyMapper.isModifierVk(vk)) {
                int flag = WebViewKeyMapper.metaForVk(vk);
                if (action == KeyboardPacket.KEY_DOWN) {
                    heldMeta |= flag;
                } else {
                    heldMeta &= ~flag;
                }
                return;
            }
            int code = WebViewKeyMapper.vkToKeyCode(vk);
            if (code < 0) {
                LimeLog.warning("Browser: unmapped VK " + vk);
                return;
            }
            long now = SystemClock.uptimeMillis();
            int act = action == KeyboardPacket.KEY_DOWN
                    ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
            int meta = heldMeta | WebViewKeyMapper.metaFromModsByte(modifiers);
            target.dispatchKeyEvent(new KeyEvent(now, now, act, code, 0, meta));
        }

        @Override
        public void text(String text) {
            View target = keyTarget();
            if (target == null || text == null || text.isEmpty()) {
                return;
            }
            // VrKeyboard sends every special character through here. A bare
            // ACTION_MULTIPLE never reaches WebView fields without an IME, so
            // expand to the real DOWN/UP sequence first (shift included).
            KeyCharacterMap map = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
            KeyEvent[] events = map.getEvents(text.toCharArray());
            if (events != null) {
                for (KeyEvent e : events) {
                    target.dispatchKeyEvent(e);
                }
                return;
            }
            // No key sequence (accents, emoji): commit at the cursor.
            InputConnection ic = target.onCreateInputConnection(new EditorInfo());
            if (ic != null && ic.commitText(text, 1)) {
                return;
            }
            target.dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), text,
                    KeyCharacterMap.VIRTUAL_KEYBOARD, 0));
        }
    }

    @Override
    protected void onDestroy() {
        if (vrKeyboard != null) {
            vrKeyboard.releaseAll();
        }
        if (xrRenderer != null) {
            xrRenderer.prepareForStop();
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (presentation != null) {
            presentation.dismiss();
            presentation = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        urlField = null;
        toolbar = null;
        customView = null;
        customCallback = null;
        favButton = null;
        rootView = null;
        if (xrRenderer != null) {
            xrRenderer.cleanup();
            xrRenderer = null;
        }
        super.onDestroy();
    }
}
