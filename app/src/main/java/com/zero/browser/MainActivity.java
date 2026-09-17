package com.zero.browser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;

    private static final String UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setUserAgentString(UA);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

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

                    if (mime.equals("text/html") && request.isForMainFrame()) {
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
                        view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
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
