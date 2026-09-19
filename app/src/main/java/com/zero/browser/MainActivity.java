package com.zero.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText addressInput;
    private ImageButton btnBack;
    private ImageButton btnForward;
    private ImageButton btnStar;
    private ImageButton btnReload;

    private static final String HOME_URL = "file:///android_asset/index.html";

    private static final String UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    private static final int BG = 0xFF08080C;
    private static final int BG2 = 0xFF0B0B11;
    private static final int BG3 = 0xFF13131C;
    private static final int TEXT = 0xFFF5F5F7;
    private static final int MUTED = 0xFF8B8B96;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            WebView.setWebContentsDebuggingEnabled(false);
        } catch (Exception ignored) {}

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarDividerColor(BG);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(false);

        // ============ ВЕРХНЯЯ ПАНЕЛЬ ============
        LinearLayout topbar = new LinearLayout(this);
        topbar.setOrientation(LinearLayout.HORIZONTAL);
        topbar.setGravity(Gravity.CENTER_VERTICAL);
        topbar.setPadding(dp(6), dp(6), dp(6), dp(6));
        topbar.setBackgroundColor(BG2);

        btnBack = makeIconBtn(android.R.drawable.ic_media_previous);
        btnBack.setEnabled(false);
        btnBack.setAlpha(0.3f);
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        topbar.addView(btnBack, new LinearLayout.LayoutParams(dp(40), dp(40)));

        btnForward = makeIconBtn(android.R.drawable.ic_media_next);
        btnForward.setEnabled(false);
        btnForward.setAlpha(0.3f);
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        topbar.addView(btnForward, new LinearLayout.LayoutParams(dp(40), dp(40)));

        addressInput = new EditText(this);
        addressInput.setHint("Поиск или адрес");
        addressInput.setSingleLine(true);
        addressInput.setTextColor(TEXT);
        addressInput.setHintTextColor(MUTED);
        addressInput.setTextSize(14);
        addressInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        addressInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressInput.setBackground(makeRoundedBg(BG3, dp(20)));
        addressInput.setPadding(dp(16), 0, dp(16), 0);
        addressInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN);
            if (enter) {
                String q = addressInput.getText().toString().trim();
                if (!q.isEmpty()) {
                    navigate(q);
                    addressInput.clearFocus();
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });

        LinearLayout.LayoutParams addrLp = new LinearLayout.LayoutParams(0, dp(40), 1);
        addrLp.setMargins(dp(4), 0, dp(4), 0);
        topbar.addView(addressInput, addrLp);

        btnStar = makeIconBtn(android.R.drawable.btn_star_big_off);
        btnStar.setOnClickListener(v -> toggleBookmark());
        topbar.addView(btnStar, new LinearLayout.LayoutParams(dp(40), dp(40)));

        btnReload = makeIconBtn(android.R.drawable.ic_menu_rotate);
        btnReload.setOnClickListener(v -> webView.reload());
        topbar.addView(btnReload, new LinearLayout.LayoutParams(dp(40), dp(40)));

        root.addView(topbar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        // ============ ЦЕНТР — WebView ============
        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setUserAgentString(UA);

        webView.setBackgroundColor(BG);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webView.setFitsSystemWindows(false);
        webView.setPadding(0, 0, 0, 0);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new ZeroBridge(), "ZeroMain");
        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!url.startsWith("http")) return null;
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", UA);
                    conn.setRequestProperty("Accept-Language", "ru,en;q=0.9");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(25000);
                    conn.setInstanceFollowRedirects(true);

                    Map<String, String> reqHeaders = request.getRequestHeaders();
                    for (Map.Entry<String, String> e : reqHeaders.entrySet()) {
                        String k = e.getKey();
                        if (k.equalsIgnoreCase("Accept-Encoding")
                            || k.equalsIgnoreCase("Host")
                            || k.equalsIgnoreCase("Connection")) continue;
                        try { conn.setRequestProperty(k, e.getValue()); } catch (Exception ignored) {}
                    }

                    int code = conn.getResponseCode();
                    String contentType = conn.getContentType();
                    if (contentType == null) contentType = "text/html";
                    String mime = contentType.split(";")[0].trim().toLowerCase();

                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", contentType);
                    headers.put("Access-Control-Allow-Origin", "*");

                    InputStream in = conn.getInputStream();

                    if (mime.equals("text/html")) {
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        byte[] chunk = new byte[8192];
                        int n;
                        while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
                        in.close();
                        String html = new String(buf.toByteArray(), "UTF-8");
                        html = html.replaceAll(
                            "(?i)<meta[^>]*http-equiv\\s*=\\s*[\"']?Content-Security-Policy[\"']?[^>]*>", "");
                        html = html.replaceAll(
                            "(?i)<meta[^>]*http-equiv\\s*=\\s*[\"']?X-Frame-Options[\"']?[^>]*>", "");
                        return new WebResourceResponse("text/html", "utf-8", code,
                            conn.getResponseMessage(), headers,
                            new ByteArrayInputStream(html.getBytes("UTF-8")));
                    }

                    return new WebResourceResponse(mime, "utf-8", code,
                        conn.getResponseMessage(), headers, in);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("mailto:") || url.startsWith("tel:")
                    || url.startsWith("sms:") || url.startsWith("intent:")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                updateUI();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                updateUI();
            }
        });

        LinearLayout.LayoutParams wvLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        root.addView(webView, wvLp);

        // ============ НИЖНЯЯ ПАНЕЛЬ ============
        LinearLayout bottombar = new LinearLayout(this);
        bottombar.setOrientation(LinearLayout.HORIZONTAL);
        bottombar.setGravity(Gravity.CENTER);
        bottombar.setBackgroundColor(BG2);
        bottombar.setPadding(0, dp(4), 0, dp(4));

        bottombar.addView(makeNavBtn("Главная", android.R.drawable.ic_menu_view, v -> {
            webView.loadUrl(HOME_URL);
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        bottombar.addView(makeNavBtn("Вкладки", android.R.drawable.ic_menu_sort_by_size, v -> {
            // заглушка
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        bottombar.addView(makeNavBtn("Закладки", android.R.drawable.btn_star_big_off, v -> {
            webView.evaluateJavascript("window.onShowBookmarks && window.onShowBookmarks()", null);
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        bottombar.addView(makeNavBtn("Ещё", android.R.drawable.ic_menu_more, v -> {
            webView.evaluateJavascript("window.onShowMenu && window.onShowMenu()", null);
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        root.addView(bottombar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        setContentView(root);

        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            webView.loadUrl(intent.getData().toString());
        } else {
            webView.loadUrl(HOME_URL);
        }
    }

    private ImageButton makeIconBtn(int iconRes) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(iconRes);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setColorFilter(TEXT);
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setPadding(dp(8), dp(8), dp(8), dp(8));
        return b;
    }

    private LinearLayout makeNavBtn(String label, int iconRes, View.OnClickListener click) {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setClickable(true);
        ll.setFocusable(true);
        ll.setOnClickListener(click);

        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setColorFilter(MUTED);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(22), dp(22));
        ll.addView(iv, ivLp);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(9.5f);
        tv.setTextColor(MUTED);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLp.topMargin = dp(3);
        ll.addView(tv, tvLp);

        return ll;
    }

    private GradientDrawable makeRoundedBg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm =
            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void navigate(String input) {
        String q = input.trim();
        if (q.isEmpty()) return;
        boolean looksUrl = !q.contains(" ") &&
                q.matches("^([a-z]+://)?([\\w-]+\\.)+[a-z]{2,}(/.*)?$");
        if (looksUrl) {
            if (!q.startsWith("http")) q = "https://" + q;
            webView.loadUrl(q);
        } else {
            webView.loadUrl("https://duckduckgo.com/?q=" + Uri.encode(q));
        }
    }

    private void updateUI() {
        runOnUiThread(() -> {
            boolean canBack = webView.canGoBack();
            boolean canForward = webView.canGoForward();

            btnBack.setEnabled(canBack);
            btnBack.setAlpha(canBack ? 1f : 0.3f);
            btnForward.setEnabled(canForward);
            btnForward.setAlpha(canForward ? 1f : 0.3f);

            String url = webView.getUrl();
            if (url != null && !url.startsWith("file://") && !url.startsWith("about:")) {
                if (!addressInput.hasFocus()) addressInput.setText(url);
            } else {
                if (!addressInput.hasFocus()) addressInput.setText("");
            }
        });
    }

    private void toggleBookmark() {
        webView.evaluateJavascript(
            "window.onToggleBookmark && window.onToggleBookmark()", null);
    }

    private class ZeroBridge {
        @android.webkit.JavascriptInterface
        public void openUrl(final String url) {
            runOnUiThread(() -> {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    webView.loadUrl(url);
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void setStar(final boolean on) {
            runOnUiThread(() -> {
                btnStar.setImageResource(on
                    ? android.R.drawable.btn_star_big_on
                    : android.R.drawable.btn_star_big_off);
                btnStar.setColorFilter(on ? 0xFFFBBF24 : TEXT);
            });
        }

        @android.webkit.JavascriptInterface
        public void clearCookies() {
            runOnUiThread(() -> {
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();
            });
        }

        @android.webkit.JavascriptInterface
        public void clearData() {
            runOnUiThread(() -> {
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();
                webView.clearCache(true);
                webView.clearHistory();
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
