package app.whatsapplite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private WebView web;
    private ValueCallback<Uri[]> filePath;
    private PermissionRequest pendingPerm;
    private View customView;
    private WebChromeClient.CustomViewCallback customCb;
    private static final int REQ_FILE = 1, REQ_PERM = 2;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setLoadWithOverviewMode(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        // تطبيق الخروج: index.html يستدعي window.App.exitApp عند الضغط على "نعم"
        web.addJavascriptInterface(new JsBridge(), "App");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    // فتح الروابط الخارجية في المتصفح، وإبقاء التطبيق على صفحته
                    if (url.contains("/android_asset/")) return false;
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception e) {}
                    return true;
                }
                return false;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            // صلاحيات الكاميرا والميكروفون للمكالمات (WebRTC)
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        pendingPerm = request;
                        if (Build.VERSION.SDK_INT >= 23) {
                            requestPermissions(new String[]{
                                android.Manifest.permission.CAMERA,
                                android.Manifest.permission.RECORD_AUDIO,
                                android.Manifest.permission.MODIFY_AUDIO_SETTINGS
                            }, REQ_PERM);
                        } else {
                            request.grant(request.getResources());
                            pendingPerm = null;
                        }
                    }
                });
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String o, GeolocationPermissions.Callback cb) {
                cb.invoke(o, true, false);
            }

            // عرض الفيديو بملء الشاشة أثناء المكالمات
            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (customView != null) { cb.onCustomViewHidden(); return; }
                customView = view; customCb = cb;
                ((FrameLayout) getWindow().getDecorView()).addView(customView,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                web.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                ((FrameLayout) getWindow().getDecorView()).removeView(customView);
                customView = null; web.setVisibility(View.VISIBLE);
                if (customCb != null) customCb.onCustomViewHidden();
            }

            // اختيار الملفات (إرسال صور/فيديو/مستندات)
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePath != null) filePath.onReceiveValue(null);
                filePath = cb;
                try {
                    Intent i = params.createIntent();
                    startActivityForResult(i, REQ_FILE);
                } catch (Exception e) { filePath = null; return false; }
                return true;
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req == REQ_PERM && pendingPerm != null) {
            pendingPerm.grant(pendingPerm.getResources());
            pendingPerm = null;
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE) {
            if (filePath == null) return;
            filePath.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            filePath = null;
        } else super.onActivityResult(req, res, data);
    }

    // زر الرجوع: يمرّره إلى منطق التطبيق (رجوع خطوة ثم تأكيد الخروج)
    @Override
    public void onBackPressed() {
        // إن كان الفيديو بملء الشاشة، أغلقه أولاً
        if (customView != null) {
            ((FrameLayout) getWindow().getDecorView()).removeView(customView);
            customView = null; web.setVisibility(View.VISIBLE);
            if (customCb != null) customCb.onCustomViewHidden();
            return;
        }
        web.evaluateJavascript(
            "(function(){ if(window.__back){ window.__back(); return true; } return false; })()",
            new ValueCallback<String>() {
                public void onReceiveValue(String v) {
                    if (!"true".equals(v)) finish();
                }
            });
    }

    class JsBridge {
        @android.webkit.JavascriptInterface
        public void exitApp() {
            runOnUiThread(new Runnable() { public void run() { finishAffinity(); } });
        }
    }
}
