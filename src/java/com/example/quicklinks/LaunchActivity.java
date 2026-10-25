package com.example.quicklinks;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class LaunchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String target = getIntent().getStringExtra("target");
        String pkg = null;
        String url = null;

        if ("chatgpt".equals(target)) {
            pkg = "com.openai.chatgpt";
            url = "https://chatgpt.com";
        } else if ("claude".equals(target)) {
            pkg = "com.anthropic.claude";
            url = "https://claude.ai";
        } else if ("deepseek".equals(target)) {
            pkg = "com.deepseek.chat";
            url = "https://chat.deepseek.com";
        } else if ("muse".equals(target)) {
            pkg = "com.facebook.aura";
            url = "https://muse.ai";
        } else if ("gemini".equals(target)) {
            pkg = "com.google.android.apps.bard";
            url = "https://gemini.google.com";
        }

        try {
            Intent launch = (pkg != null)
                    ? getPackageManager().getLaunchIntentForPackage(pkg)
                    : null;
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
            } else {
                openWeb(url);
            }
        } catch (ActivityNotFoundException e) {
            openWeb(url);
        } finally {
            finish();
        }
    }

    private void openWeb(String url) {
        if (url == null) return;
        try {
            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(web);
        } catch (ActivityNotFoundException e) {
            // No browser available either; nothing more we can do.
        }
    }
}
