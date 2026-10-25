package com.example.quicklinks;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class QuickLinksWidget extends AppWidgetProvider {

    // Layout this provider renders. Subclasses (the 4x1 strip) override
    // this to supply their own layout; button IDs must match.
    protected int getLayoutRes() {
        return R.layout.widget_layout;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            RemoteViews v = new RemoteViews(context.getPackageName(), getLayoutRes());
            setLink(context, v, R.id.btn_chatgpt,  "chatgpt");
            setLink(context, v, R.id.btn_claude,   "claude");
            setLink(context, v, R.id.btn_deepseek, "deepseek");
            setLink(context, v, R.id.btn_muse,     "muse");
            setLink(context, v, R.id.btn_gemini,   "gemini");
            mgr.updateAppWidget(id, v);
        }
    }

    private void setLink(Context ctx, RemoteViews v, int viewId, String target) {
        // Each pill now opens LaunchActivity, which decides at tap time:
        // native app if installed, website as fallback.
        Intent i = new Intent(ctx, LaunchActivity.class);
        i.putExtra("target", target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(
                ctx, viewId, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(viewId, pi);
    }
}
