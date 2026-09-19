package com.zero.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.FrameLayout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView mainWV;
    private WebView topWV;
    private WebView botWV;

    private static final String HOME_URL = "file:///android_asset/index.html";
    private static final String TOP_URL = "file:///android_asset/top.html";
    private static final String BOTTOM_URL = "file:///android_asset/bottom.html";

    private static final String UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    private static final int BG = 0xFF08080C;
    private static final int TOP_H = 58;
    private static final int BOTTOM_H = 58;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try { WebView.setWebContentsDebuggingEnabled(false); } catch (Exception ignored) {}

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarDividerColor(BG);
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(false);

        int topH = dp(TOP_H);
        int botH = dp(BOTTOM_H);

        topWV = createWV();
        topWV.addJavascriptInterface(new TopBridge(), "ZeroTop");
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, topH);
        topLp.gravity = Gravity.TOP;
        root.addView(topWV, topLp);

        botWV = createWV();
        botWV.addJavascriptInterface(new BottomBridge(), "ZeroBottom");
        FrameLayout.LayoutParams botLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, botH);
        botLp.gravity = Gravity.BOTTOM;
        root.addView(botWV, botLp);

        mainWV = createWV();
        mainWV.addJavascriptInterface(new MainBridge(), "ZeroMain");
        FrameLayout.LayoutParams mainLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mainLp.topMargin = topH;
        mainLp.bottomMargin = botH;
        root.addView(mainWV, mainLp);

        setupClients();
        setContentView(root);

        topWV.loadUrl(TOP_URL);
        botWV.loadUrl(BOTTOM_URL);
        mainWV.loadUrl(HOME_URL);
    }

    private WebView createWV() {
        WebView wv = new WebView(this);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(UA);
        wv.setBackgroundColor(BG);
        wv.setVerticalScrollBarEnabled(false);
        wv.setHorizontalScrollBarEnabled(false);
        wv.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        wv.setFitsSystemWindows(false);
        wv.setPadding(0, 0, 0, 0);
        return wv;
    }

    private void setupClients() {
        mainWV.setWebViewClient(new WebViewClient() {
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
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                    catch (Exception ignored) {}
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                updateTopBar(url, view.getTitle(), view.canGoBack(), view.canGoForward());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                updateTopBar(url, view.getTitle(), view.canGoBack(), view.canGoForward());
            }
        });

        mainWV.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                updateTopBar(view.getUrl(), title, view.canGoBack(), view.canGoForward());
            }
        });
    }

    private void updateTopBar(String url, String title, boolean canBack, boolean canForward) {
        final String safeUrl = url == null ? "" : url.replace("\\", "\\\\").replace("'", "\\'");
        final String safeTitle = title == null ? "" : title.replace("\\", "\\\\").replace("'", "\\'");
        final String js = "if(window.setAddress) window.setAddress('" + safeUrl + "','" + safeTitle + "',"
            + canBack + "," + canForward + ");";
        topWV.post(() -> topWV.evaluateJavascript(js, null));
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private class TopBridge {
        @JavascriptInterface
        public void openUrl(final String url) {
            runOnUiThread(() -> {
                if (url == null || url.isEmpty()) return;
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    mainWV.loadUrl(url);
                } else if (isUrlLike(url)) {
                    mainWV.loadUrl("https://" + url);
                } else {
                    mainWV.loadUrl("https://duckduckgo.com/?q=" + Uri.encode(url));
                }
            });
        }

        @JavascriptInterface
        public void goBack() {
            runOnUiThread(() -> { if (mainWV.canGoBack()) mainWV.goBack(); });
        }

        @JavascriptInterface
        public void goForward() {
            runOnUiThread(() -> { if (mainWV.canGoForward()) mainWV.goForward(); });
        }

        @JavascriptInterface
        public void reload() {
            runOnUiThread(() -> mainWV.reload());
        }

        @JavascriptInterface
        public void toggleBookmark() {
            runOnUiThread(() -> mainWV.evaluateJavascript(
                "window.onToggleBookmark && window.onToggleBookmark()", null));
        }

        private boolean isUrlLike(String s) {
            if (s.contains(" ")) return false;
            return s.matches("^([a-z]+://)?([\\w-]+\\.)+[a-z]{2,}(/.*)?$");
        }
    }

    private class BottomBridge {
        @JavascriptInterface
        public void goHome() {
            runOnUiThread(() -> mainWV.loadUrl(HOME_URL));
        }
        @JavascriptInterface
        public void showTabs() {
            runOnUiThread(() -> mainWV.evaluateJavascript(
                "window.onShowTabs && window.onShowTabs()", null));
        }
        @JavascriptInterface
        public void showBookmarks() {
            runOnUiThread(() -> mainWV.evaluateJavascript(
                "window.onShowBookmarks && window.onShowBookmarks()", null));
        }
        @JavascriptInterface
        public void showMenu() {
            runOnUiThread(() -> mainWV.evaluateJavascript(
                "window.onShowMenu && window.onShowMenu()", null));
        }
    }

    private class MainBridge {
        @JavascriptInterface
        public void setStar(final boolean on) {
            runOnUiThread(() -> topWV.evaluateJavascript(
                "window.setStar && window.setStar(" + on + ")", null));
        }

        @JavascriptInterface
        public void openUrl(final String url) {
            runOnUiThread(() -> {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    mainWV.loadUrl(url);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (mainWV.canGoBack()) mainWV.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mainWV.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainWV.onResume();
    }

    @Override
    protected void onDestroy() {
        mainWV.destroy();
        topWV.destroy();
        botWV.destroy();
        super.onDestroy();
    }
                            }
