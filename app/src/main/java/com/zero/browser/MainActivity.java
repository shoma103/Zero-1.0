package com.zero.browser;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;
    private static final String HOME_URL = "file:///android_asset/index.html";
    private static final int MIC_PERMISSION_CODE = 101;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

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

        webView.addJavascriptInterface(new ZeroBridge(), "ZeroAndroid");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    for (String resource : request.getResources()) {
                        if (resource.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                            if (ContextCompat.checkSelfPermission(MainActivity.this,
                                    Manifest.permission.RECORD_AUDIO)
                                    == PackageManager.PERMISSION_GRANTED) {
                                request.grant(request.getResources());
                            }
                        }
                    }
                });
            }
        });

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

        initSpeechRecognizer();

        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            webView.loadUrl(intent.getData().toString());
        } else {
            webView.loadUrl(HOME_URL);
        }

        setContentView(webView);
    }

    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                sendToJS("onSpeechReady", "");
            }
            @Override public void onBeginningOfSpeech() {
                sendToJS("onSpeechStart", "");
            }
            @Override public void onRmsChanged(float rmsdB) {
                float normalized = Math.max(0, Math.min(1, (rmsdB + 2) / 12f));
                sendToJS("onSpeechVolume", String.valueOf(normalized));
            }
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                sendToJS("onSpeechEnd", "");
            }
            @Override public void onError(int error) {
                sendToJS("onSpeechError", String.valueOf(error));
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    sendToJS("onSpeechResult", matches.get(0));
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    sendToJS("onSpeechPartial", matches.get(0));
                }
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void sendToJS(String method, String value) {
        if (webView == null) return;
        final String js = "window." + method + " && window." + method + "(" +
                escapeJsString(value) + ");";
        runOnUiThread(() -> webView.evaluateJavascript(js, null));
    }

    private String escapeJsString(String s) {
        if (s == null) return "''";
        return "'" + s.replace("\\", "\\\\")
                       .replace("'", "\\'")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r") + "'";
    }

    private void requestMicAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startSpeech();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
        }
    }

    private void startSpeech() {
        if (speechRecognizer == null) {
            initSpeechRecognizer();
        }
        if (speechRecognizer == null) {
            sendToJS("onSpeechError", "not_available");
            return;
        }
        try {
            speechRecognizer.startListening(speechIntent);
        } catch (Exception e) {
            sendToJS("onSpeechError", "start_failed");
        }
    }

    private void stopSpeech() {
        if (speechRecognizer != null) {
            try { speechRecognizer.stopListening(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeech();
            } else {
                sendToJS("onSpeechError", "permission_denied");
            }
        }
    }

    private class ZeroBridge {

        @JavascriptInterface
        public void openUrl(final String url) {
            runOnUiThread(() -> {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    webView.loadUrl(url);
                }
            });
        }

        @JavascriptInterface
        public void goBack() {
            runOnUiThread(() -> { if (webView.canGoBack()) webView.goBack(); });
        }

        @JavascriptInterface
        public void goHome() {
            runOnUiThread(() -> webView.loadUrl(HOME_URL));
        }

        @JavascriptInterface
        public boolean isSpeechAvailable() {
            return speechRecognizer != null;
        }

        @JavascriptInterface
        public void startSpeechRecognition() {
            runOnUiThread(MainActivity.this::requestMicAndStart);
        }

        @JavascriptInterface
        public void stopSpeechRecognition() {
            runOnUiThread(MainActivity.this::stopSpeech);
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
        if (intent != null && intent.getData() != null) {
            webView.loadUrl(intent.getData().toString());
        }
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
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        webView.destroy();
        super.onDestroy();
    }
}
