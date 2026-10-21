package com.pocketledger.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int EXPORT = 100, IMPORT = 101;
    private static final String ORIGIN = "https://app.pocketledger.local/";
    private WebView web;
    private SharedPreferences prefs;
    private String pendingExport;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("ledger", MODE_PRIVATE);
        if (!prefs.getBoolean("treeFreshStart", false)) {
            prefs.edit().remove("data").remove("previousData").putBoolean("treeFreshStart",true).commit();
        }
        ReminderReceiver.schedule(this);
        if (saved != null) pendingExport = saved.getString("pendingExport");
        getWindow().setStatusBarColor(Color.rgb(23,76,61));
        // Resize the WebView's parent, rather than padding the WebView itself:
        // Chromium's viewport must exclude the system bars and keyboard.
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.setBackgroundColor(Color.rgb(16,30,28));
        web = new WebView(this);
        container.addView(web,new android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        container.setOnApplyWindowInsetsListener((v,insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars() |
                    android.view.WindowInsets.Type.displayCutout() |
                    android.view.WindowInsets.Type.ime());
                v.setPadding(safe.left,safe.top,safe.right,safe.bottom);
                return new android.view.WindowInsets.Builder(insets)
                    .setInsets(android.view.WindowInsets.Type.systemBars() |
                        android.view.WindowInsets.Type.displayCutout() |
                        android.view.WindowInsets.Type.ime(),android.graphics.Insets.NONE)
                    .setDisplayCutout(null).build();
            }
            int left=insets.getSystemWindowInsetLeft(),top=insets.getSystemWindowInsetTop();
            int right=insets.getSystemWindowInsetRight(),bottom=insets.getSystemWindowInsetBottom();
            if (android.os.Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout()!=null) {
                android.view.DisplayCutout cutout=insets.getDisplayCutout();
                left=Math.max(left,cutout.getSafeInsetLeft());
                top=Math.max(top,cutout.getSafeInsetTop());
                right=Math.max(right,cutout.getSafeInsetRight());
                bottom=Math.max(bottom,cutout.getSafeInsetBottom());
            }
            v.setPadding(left,top,right,bottom);
            return insets.replaceSystemWindowInsets(0,0,0,0);
        });
        setContentView(container);
        container.requestApplyInsets();
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new StorageBridge(), "AndroidLedger");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest r) {
                return !r.getUrl().toString().equals(ORIGIN + "index.html");
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest r) {
                String url = r.getUrl().toString();
                if (url.startsWith(ORIGIN)) {
                    String name = r.getUrl().getPath();
                    if (name != null && name.matches("/(index.html|app.js|ledger.js|icon.svg|tree.png)")) {
                        try {
                            String mime = name.endsWith(".png") ? "image/png" : name.endsWith(".js") ? "text/javascript" : name.endsWith(".svg") ? "image/svg+xml" : "text/html";
                            return new WebResourceResponse(mime,"UTF-8",getAssets().open(name.substring(1)));
                        } catch (IOException ignored) { }
                    }
                }
                return new WebResourceResponse("text/plain","UTF-8",404,"Not found",null,new ByteArrayInputStream(new byte[0]));
            }
        });
        web.loadUrl(ORIGIN + "index.html");
    }

    public class StorageBridge {
        @JavascriptInterface public void enableReminder() {
            runOnUiThread(() -> {
                prefs.edit().putBoolean("reminder",true).apply();
                if(android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},25);
                } else configureAlarm();
            });
        }
        @JavascriptInterface public String load() { return prefs.getString("data", ""); }
        @JavascriptInterface public boolean save(String json) {
            if (json == null || json.length() > 10000000) return false;
            try {
                new JSONObject(json);
                SharedPreferences.Editor edit = prefs.edit();
                String previous = prefs.getString("data", "");
                if (!previous.isEmpty()) edit.putString("previousData", previous);
                return edit.putString("data",json).commit();
            }
            catch (Exception e) { return false; }
        }
        @JavascriptInterface public void exportBackup(String json) {
            if (json == null || json.length() > 10000000) return;
            runOnUiThread(() -> {
                pendingExport = json;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE,"Pocket-Ledger-Backup.json");
                try { startActivityForResult(intent,EXPORT); }
                catch (Exception e) { message("No file picker is available."); }
            });
        }
        @JavascriptInterface public void importBackup() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try { startActivityForResult(intent,IMPORT); }
                catch (Exception e) { message("No file picker is available."); }
            });
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == EXPORT && pendingExport != null) {
            final String backup = pendingExport;
            pendingExport = null;
            new Thread(() -> {
                try (OutputStream out = getContentResolver().openOutputStream(uri,"wt")) {
                    if (out == null) throw new IOException();
                    out.write(backup.getBytes(StandardCharsets.UTF_8));
                    message("Backup exported.");
                } catch (Exception e) { message("Could not export backup."); }
            }).start();
        } else if (request == IMPORT) {
            new Thread(() -> {
                try (InputStream in = getContentResolver().openInputStream(uri);
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    if (in == null) throw new IOException();
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) != -1) {
                        if (out.size() + n > 10000000) throw new IOException();
                        out.write(buffer,0,n);
                    }
                    String json = out.toString("UTF-8");
                    runOnUiThread(() -> web.evaluateJavascript("window.receiveAndroidBackup(" + JSONObject.quote(json) + ");",null));
                } catch (Exception e) { message("Cannot read backup; choose a JSON file under 10 MB."); }
            }).start();
        }
    }
    private void configureAlarm() {
        ReminderReceiver.schedule(this);
        android.app.AlarmManager am=(android.app.AlarmManager)getSystemService(ALARM_SERVICE);
        if(android.os.Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) {
            try { startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName()))); }
            catch(Exception e) { message("Reminder enabled; Android can delay delivery without exact alarm access."); }
        } else message("Monthly reminder enabled.");
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==25 && results.length>0 && results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED) configureAlarm();
        else if(request==25) message("Notifications are blocked. Enable them in Android settings to receive reminders.");
    }
    @Override protected void onResume() { super.onResume(); ReminderReceiver.schedule(this); }
    private void message(String text) { runOnUiThread(() -> Toast.makeText(this,text,Toast.LENGTH_LONG).show()); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); if(pendingExport != null) out.putString("pendingExport",pendingExport); }
    @Override protected void onDestroy() { if(web != null) { web.removeJavascriptInterface("AndroidLedger"); web.destroy(); } super.onDestroy(); }
}
