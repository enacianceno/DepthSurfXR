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
        int barPx = Math.round(52 * density);
        int padPx = Math.round(6 * density);
        toolbar.setPadding(padPx, padPx, padPx, padPx);

        Button back = navButton(ctx, "<");
        back.setOnClickListener(v -> {
            if (customView != null) {
                exitFullscreen();
            } else if (webView.canGoBack()) {
                webView.goBack();
            }
        });
        Button fwd = navButton(ctx, ">");
        fwd.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });
        Button reload = navButton(ctx, "R");
        reload.setOnClickListener(v -> webView.reload());
        Button home = navButton(ctx, "H");
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

        Button clear = navButton(ctx, "X");
        clear.setOnClickListener(v -> urlField.setText(""));
        Button paste = navButton(ctx, "P");
        paste.setOnClickListener(v -> pasteAndGo());
        Button go = navButton(ctx, "Ir");
        go.setOnClickListener(v -> goFromBar());
        favButton = navButton(ctx, "*");
        favButton.setOnClickListener(v -> saveFavorite());
        Button favList = navButton(ctx, "F");
        favList.setOnClickListener(v -> showFavorites());

        toolbar.addView(back);
        toolbar.addView(fwd);
        toolbar.addView(reload);
        toolbar.addView(home);
        toolbar.addView(urlField, urlLp);
        toolbar.addView(clear);
        toolbar.addView(paste);
        toolbar.addView(go);
        toolbar.addView(favButton);
        toolbar.addView(favList);
        rootView.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, barPx));

        webView = new WebView(ctx);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        // Large sites restrict the stock "; wv" WebView token (TikTok serves
        // a degraded wall to it): present as plain Chrome instead.
        webView.getSettings().setUserAgentString(BrowserUa.desktopChrome(
                webView.getSettings().getUserAgentString()));
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
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
                if (message != null
                        && message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    LimeLog.warning("BrowserDiag console level=ERROR msg="
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

        presentation.setContentView(rootView,
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
        webView.loadUrl(HOME_URL);
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

    @Override
    public void onVrGamepadState(int buttons, int leftTrigger, int rightTrigger,
                                 int leftX, int leftY, int rightX, int rightY) {
        // No gamepad in a browser.
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
            webView.scrollBy(0, WebViewInputMapper.scrollPixels(clicks));
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
        rootView.dispatchTouchEvent(event);
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
