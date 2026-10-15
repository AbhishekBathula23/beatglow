package com.beatglow.studio;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int REQ_FILE = 1;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    // state for saving a file coming from the web page
    private OutputStream out;
    private Uri outUri;
    private String outName = "";
    private String outDir = Environment.DIRECTORY_DOWNLOADS;
    private String outError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF07060F);
        getWindow().setNavigationBarColor(0xFF07060F);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);

        // Serve the bundled web app over https so WebCodecs and other secure-context APIs work.
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;

                ArrayList<String> mimes = new ArrayList<>();
                String[] accept = params.getAcceptTypes();
                if (accept != null) {
                    for (String t : accept) {
                        if (t == null) continue;
                        for (String x : t.split(",")) {
                            x = x.trim();
                            if (x.contains("/") && !mimes.contains(x)) mimes.add(x);
                        }
                    }
                }

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                if (mimes.size() == 1) {
                    i.setType(mimes.get(0));
                } else {
                    i.setType("*/*");
                    if (!mimes.isEmpty()) i.putExtra(Intent.EXTRA_MIME_TYPES, mimes.toArray(new String[0]));
                }
                try {
                    startActivityForResult(Intent.createChooser(i, "Choose file"), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getData() != null) {
                    result = new Uri[]{data.getData()};
                } else if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    result = new Uri[n];
                    for (int k = 0; k < n; k++) result[k] = data.getClipData().getItemAt(k).getUri();
                }
            }
            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }

    /** Methods the web page calls through window.AndroidBridge. */
    private class Bridge {

        @JavascriptInterface
        public void begin(String name, String mime) {
            outError = null;
            out = null;
            try {
                outName = name == null ? "beatglow" : name.replaceAll("[\\\\/:*?\"<>|]", "_");
                String m = (mime == null || mime.isEmpty()) ? "application/octet-stream" : mime;
                boolean video = m.startsWith("video/");
                boolean image = m.startsWith("image/");
                outDir = video ? Environment.DIRECTORY_MOVIES
                        : image ? Environment.DIRECTORY_PICTURES
                        : Environment.DIRECTORY_DOWNLOADS;

                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, outName);
                v.put(MediaStore.MediaColumns.MIME_TYPE, m);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, outDir + "/BeatGlow");
                v.put(MediaStore.MediaColumns.IS_PENDING, 1);

                Uri collection = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        : image ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        : MediaStore.Downloads.EXTERNAL_CONTENT_URI;

                ContentResolver cr = getContentResolver();
                outUri = cr.insert(collection, v);
                out = cr.openOutputStream(outUri);
            } catch (Exception e) {
                outError = String.valueOf(e.getMessage());
            }
        }

        @JavascriptInterface
        public void chunk(String base64) {
            if (out == null || outError != null) return;
            try {
                out.write(Base64.decode(base64, Base64.DEFAULT));
            } catch (Exception e) {
                outError = String.valueOf(e.getMessage());
            }
        }

        @JavascriptInterface
        public String end() {
            try {
                if (out != null) {
                    out.close();
                    out = null;
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    getContentResolver().update(outUri, v, null, null);
                }
            } catch (Exception e) {
                outError = String.valueOf(e.getMessage());
            }
            return outError == null
                    ? "Saved to " + outDir + "/BeatGlow/" + outName
                    : "Could not save: " + outError;
        }

        @JavascriptInterface
        public void keepAwake(final boolean on) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            });
        }
    }
}
