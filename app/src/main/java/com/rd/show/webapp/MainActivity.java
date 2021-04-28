package com.rd.show.webapp;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// implements DeviceKeyMonitor.OnKeyListener
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private TokenSaver tokenSaver;
    //    private WebSettings webSettings;
    private static final String TAG = "MainActivity";
    private JSONObject jsobj;
    private String token;

//    private ValueCallback<Uri> mUploadMessage;
//    public ValueCallback<Uri[]> uploadMessage;
//    public static final int REQUEST_SELECT_FILE = 100;
//    private final static int FILECHOOSER_RESULTCODE = 2;

//    private DeviceKeyMonitor mKeyMonitor;

    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint({"SetJavaScriptEnabled", "SourceLockedOrientationActivity"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);  //螢幕保持直向
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //螢幕保持橫向
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); //設定螢幕不隨手機旋轉

//        mKeyMonitor = new DeviceKeyMonitor(this, this);

        //        手機瀏海區
        WindowManager.LayoutParams lpp = getWindow().getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lpp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        }
        getWindow().setAttributes(lpp);

        // 隱藏虛擬按鍵，並且全屏
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    // 虛擬按鍵出現要做的事情
                    hideBottomUIMenu();
                }  // 虛擬按鍵消失後要做的事情

            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }


        webView = new WebView(this);

        FrameLayout rootView = findViewById(R.id.xxx);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rootView.addView(webView, layoutParams);

        webView.setWebViewClient( new Browser_home());
        webView.setWebChromeClient( new MyChrome());
        //声明WebSettings子类
        WebSettings webSettings = webView.getSettings();
        //如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        webSettings.setJavaScriptEnabled(true);
        // 若載入的 html 裡有JS 在執行動畫等操作，會造成資源浪費（CPU、電量）
        // 在 onStop 和 onResume 裡分別把 setJavaScriptEnabled() 給設定成 false 和 true 即可

        //設定自適應螢幕，兩者合用
        webSettings.setUseWideViewPort(true); //將圖片調整到適合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 縮放至螢幕的大小
        //縮放操作
        webSettings.setSupportZoom(false); // 支持縮放, 默認為true. 是下面那個的前提.
        webSettings.setBuiltInZoomControls(false); // 設置內置的縮放控件. 若為false, 則該WebView不可縮放
        webSettings.setDisplayZoomControls(false); // 隱藏原生的縮放控件
        // 關閉密碼保存提醒(false)  開啟密碼保存功能(true)
        webSettings.setSavePassword(false);
        // 是否支持多窗口，默認值false
        webSettings.setSupportMultipleWindows(false);
        // 是否可用Javascript(window.open)打開窗口，默認值 false
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setAllowContentAccess(true); // 是否可訪問Content Provider的資源，默認值 true
        // 設定可以訪問檔案
        webSettings.setAllowFileAccess(true);
        // 是否允許通過file url加載的Javascript讀取本地文件，默認值 false
        webSettings.setAllowFileAccessFromFileURLs(false);
        // 是否允許通過file url加載的Javascript讀取全部資源(包括文件,http,https)，默認值 false
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        // 支援通過JS開啟新視窗
//        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

//        webSettings.setAppCacheEnabled(true);

        webView.requestFocus(View.FOCUS_DOWN);

        //自動播放影片
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        jsobj = new JSONObject();

        loadWebsite();

//        clearCookies(this);


    }

    protected void hideBottomUIMenu() {
        //隱藏虛擬按鍵，並且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);

        }
    }

//    private void showBottomUIMenu(){
//        //恢复普通状态
//        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
//            View v = this.getWindow().getDecorView();
//            v.setSystemUiVisibility(View.VISIBLE);
//        } else if (Build.VERSION.SDK_INT >= 19) {
//            //for new api versions.
//            View decorView = getWindow().getDecorView();
//            int uiOptions = View.SCREEN_STATE_OFF;
//            decorView.setSystemUiVisibility(uiOptions);
//        }
//    }

    private void loadWebsite() {
        webView.loadUrl("網址");

    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        Log.i("VVVVVVVV", "onResume: ");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

//    @Override
//    public void onHomeClick() {
//        Log.d("DeviceKeyMonitor", "按了Home键");
//    }
//
//    @Override
//    public void onRecentClick() {
//        Log.d("DeviceKeyMonitor", "按了多任务键");
//    }

    class Browser_home extends WebViewClient {
        Browser_home() {

        }

        // 开始载入页面时调用此方法，在这里我们可以设定一个loading的页面，告诉用户程序正在等待网络响应。
        @SuppressLint("SourceLockedOrientationActivity")
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d("view", "開始載入");
            Log.i(TAG, "onPageStarted:　");

            if (url.contains("fifteen")) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE); //螢幕保持橫向
            } else if (url.contains("fish")) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE); //螢幕保持橫向
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);  //螢幕保持直向
            }

        }

        // 在页面加载结束时调用。我们可以关闭loading 条，切换程序动作。
        @Override
        public void onPageFinished(WebView view, String url) {

            Log.d("view", "載入結束");
            Log.i(TAG, "onPageFinished: ");

            setTitle(view.getTitle());
            CookieManager cookieManager = CookieManager.getInstance();
            String CookieStr = cookieManager.getCookie(url);
            Log.i(TAG, "CookieStr : " + CookieStr);

            Log.i(TAG, "onPageFinished: view : " + view);

            TokenSaver.getToken(webView.getContext());

            Log.i(TAG, "onPageFinished: url : " + url);

            super.onPageFinished(view, url);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
//            Log.i(TAG, "onLoadResource1 : " + view);
//            Log.i(TAG, "onLoadResource2 : " + url);
        }

        // 連結跳轉都會走這個方法
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i(TAG, "shouldOverrideUrlLoading : " + url);

//            view.loadUrl(url);
//            CookieManager cm = CookieManager.getInstance();
//            cm.setAcceptCookie(true);

            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (request.isForMainFrame()) {
                    Toast.makeText(getApplicationContext(), "網路連線有問題", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "-網路連線有問題-");
                }
            }
        }
    }

    private class MyChrome extends WebChromeClient {

        private View mCustomView;
        private WebChromeClient.CustomViewCallback mCustomViewCallback;
        protected FrameLayout mFullscreenContainer;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        MyChrome() {
        }

        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView() {
            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams( -1, -1));
            getWindow().getDecorView().setSystemUiVisibility(3846);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            AlertDialog.Builder b = new AlertDialog.Builder( MainActivity.this);
            b.setMessage(message);
            b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            });
            b.setCancelable(false);
            b.create().show();
            return true;
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
            Log.i(TAG , "icon : " + icon);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            Log.i(TAG , "title : " + title);
        }

//        // WebView 不支持 H5 input type="file" 解决方法
//        // For 3.0+ Devices (Start)
//        // onActivityResult attached before constructor
//        protected void openFileChooser(ValueCallback uploadMsg, String acceptType)
//        {
//            mUploadMessage = uploadMsg;
//            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
//            i.addCategory(Intent.CATEGORY_OPENABLE);
//            i.setType("image/*");
//            startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE);
//        }
//        // For Lollipop 5.0+ Devices
//        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//        public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams)
//        {
//            if (uploadMessage != null) {
//                uploadMessage.onReceiveValue(null);
//                uploadMessage = null;
//            }
//            uploadMessage = filePathCallback;
//            Intent intent = fileChooserParams.createIntent();
//            try
//            {
//                startActivityForResult(intent, REQUEST_SELECT_FILE);
//            } catch (ActivityNotFoundException e)
//            {
//                uploadMessage = null;
//                Toast.makeText(getBaseContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
//                return false;
//            }
//            return true;
//        }
//        //For Android 4.1 only
//        protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
//        {
//            mUploadMessage = uploadMsg;
//            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//            intent.addCategory(Intent.CATEGORY_OPENABLE);
//            intent.setType("image/*");
//            startActivityForResult(Intent.createChooser(intent, "File Browser"), FILECHOOSER_RESULTCODE);
//        }
//        protected void openFileChooser(ValueCallback<Uri> uploadMsg)
//        {
//            mUploadMessage = uploadMsg;
//            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
//            i.addCategory(Intent.CATEGORY_OPENABLE);
//            i.setType("image/*");
//            startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
//        }

        // For Android < 3.0
        public void openFileChooser(ValueCallback<Uri> valueCallback) {
            uploadMessage = valueCallback;
            openImageChooserActivity();
        }

        // For Android  >= 3.0
        public void openFileChooser(ValueCallback valueCallback, String acceptType) {
            uploadMessage = valueCallback;
            openImageChooserActivity();
        }

        //For Android  >= 4.1
        public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
            uploadMessage = valueCallback;
            openImageChooserActivity();
        }

        // For Android >= 5.0
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            uploadMessageAboveL = filePathCallback;
            openImageChooserActivity();
            return true;
        }

    }

    private void openImageChooserActivity() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILE_CHOOSER_RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessage && null == uploadMessageAboveL) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (uploadMessage != null) {
                uploadMessage.onReceiveValue(result);
                uploadMessage = null;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode,  Intent intent) {
//
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//        {
//            if (requestCode == REQUEST_SELECT_FILE)
//            {
//                if (uploadMessage == null)
//                    return;
//                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
//                uploadMessage = null;
//            }
//        }
//        else if (requestCode == FILECHOOSER_RESULTCODE)
//        {
//            if (null == mUploadMessage)
//                return;
//            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
//            // Use RESULT_OK only if you're implementing WebView inside an Activity
//            Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
//            mUploadMessage.onReceiveValue(result);
//            mUploadMessage = null;
//        }
//        else
//            Toast.makeText(getBaseContext(), "Failed to Upload Image", Toast.LENGTH_LONG).show();
//    }

    private class JsObject {
        // 此方法被js呼叫
//        @SuppressLint("JavascriptInterface")
//        @JavascriptInterface
//        public void showbachin(String url, int pid, int mid) {
//
//            Intent intent = new Intent();
//            Bundle bundle = new Bundle();
//            bundle.putString("url", url);
//            bundle.putInt("pid", pid);
//            bundle.putInt("mid", mid);
//            intent.putExtras(bundle);
//            intent.setClass(MainActivity.this, Game3Activity.class);
//            startActivity(intent);
//        }
//
//        @JavascriptInterface
//        public void showslot(String url, int pid, int mid) {
//
//            Intent intent = new Intent();
//            Bundle bundle = new Bundle();
//            bundle.putString("url", url);
//            bundle.putInt("pid", pid);
//            bundle.putInt("mid", mid);
//            intent.putExtras(bundle);
//            intent.setClass(MainActivity.this, Game1Activity.class);
//            startActivity(intent);
//        }
//
//        @JavascriptInterface
//        public void showFish(String url, int pid, int sid, int mid) {
//
//            Intent intent = new Intent();
//            Bundle bundle = new Bundle();
//            bundle.putString("url", url);
//            bundle.putInt("pid", pid);
//            bundle.putInt("sid", sid);
//            bundle.putInt("mid", mid);
//            intent.putExtras(bundle);
//            intent.setClass(MainActivity.this, Game2Activity.class);
//            startActivity(intent);
//        }
    }

    @Override
    protected void onDestroy() {
//        mKeyMonitor.unregister();
        super.onDestroy();
        ((ViewGroup) webView.getParent()).removeView(webView);
        webView.setTag(null);

        //清除当前webview访问的历史记录
        //只会webview访问历史记录里的所有记录除了当前访问记录
        webView.clearHistory();

        //这个api仅仅清除自动完成填充的表单数据，并不会清除WebView存储到本地的数据
        webView.clearFormData();

        //清除网页访问留下的缓存
        //由于内核缓存是全局的因此这个方法不仅仅针对webview而是针对整个应用程序.
        webView.clearCache(true);
        clearCookies(this);
        webView.destroy();
        webView = null;

    }

    public void clearCookies(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.startSync();
            cookieSyncMngr.sync();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
