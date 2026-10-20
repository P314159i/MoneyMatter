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
        if (saved != null) pendingExport = saved.getString("pendingExport");
        getWindow().setStatusBarColor(Color.rgb(23,76,61));
        web = new WebView(this);
        setContentView(web);
        web.setPadding(0,0,0,0);
        web.setOnApplyWindowInsetsListener((v,insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
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
                    if (name != null && name.matches("/(index.html|app.js|ledger.js|icon.svg)")) {
                        try {
                            String mime = name.endsWith(".js") ? "text/javascript" : name.endsWith(".svg") ? "image/svg+xml" : "text/html";
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
        @JavascriptInterface public String load() { return prefs.getString("data", ""); }
        @JavascriptInterface public boolean save(String json) {
            if (json == null || json.length() > 10000000) return false;
            try { new JSONObject(json); return prefs.edit().putString("data",json).commit(); }
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
    private void message(String text) { runOnUiThread(() -> Toast.makeText(this,text,Toast.LENGTH_LONG).show()); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); if(pendingExport != null) out.putString("pendingExport",pendingExport); }
    @Override protected void onDestroy() { if(web != null) { web.removeJavascriptInterface("AndroidLedger"); web.destroy(); } super.onDestroy(); }
}
