package com.example.quicklinks;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.CalendarContract;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Beauty Calendar widget: a real computed month calendar (Mon-first) with
 * event dots, plus a Quick Controls panel.
 *
 * Why the toggles can work here: this app targets API 28. Android blocks
 * WifiManager.setWifiEnabled for apps targeting 29+ and
 * BluetoothAdapter.enable() for 33+, which is why Play Store widget apps
 * (KWGT included) had to drop those toggles. Targeting 28 keeps them legal.
 */
public class BeautyCalendarWidget extends AppWidgetProvider {

    public static final String ACTION_PREV = "com.example.quicklinks.CAL_PREV";
    public static final String ACTION_NEXT = "com.example.quicklinks.CAL_NEXT";
    public static final String ACTION_TOGGLE_WIFI = "com.example.quicklinks.TOGGLE_WIFI";
    public static final String ACTION_TOGGLE_BT = "com.example.quicklinks.TOGGLE_BT";
    public static final String ACTION_TOGGLE_TORCH = "com.example.quicklinks.TOGGLE_TORCH";
    public static final String ACTION_REFRESH = "com.example.quicklinks.CAL_REFRESH";

    private static final String PREFS = "beauty_cal";
    private static final String KEY_OFFSET = "month_offset";
    private static final String KEY_TORCH = "torch_on";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            manager.updateAppWidget(id, buildViews(context));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_PREV.equals(action) || ACTION_NEXT.equals(action)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int offset = prefs.getInt(KEY_OFFSET, 0) + (ACTION_PREV.equals(action) ? -1 : 1);
            prefs.edit().putInt(KEY_OFFSET, offset).apply();
            refreshAll(context);
        } else if (ACTION_TOGGLE_WIFI.equals(action)) {
            toggleWifi(context);
            refreshAll(context);
        } else if (ACTION_TOGGLE_BT.equals(action)) {
            toggleBluetooth(context);
            refreshAll(context);
        } else if (ACTION_TOGGLE_TORCH.equals(action)) {
            toggleTorch(context);
            refreshAll(context);
        } else if (ACTION_REFRESH.equals(action)
                || WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)
                || BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            refreshAll(context);
        } else {
            super.onReceive(context, intent);
        }
    }

    private void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, BeautyCalendarWidget.class));
        for (int id : ids) {
            manager.updateAppWidget(id, buildViews(context));
        }
    }

    // ------------------------------------------------------------------ UI

    private RemoteViews buildViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_calendar);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int offset = prefs.getInt(KEY_OFFSET, 0);

        Calendar shown = Calendar.getInstance();
        shown.add(Calendar.MONTH, offset);
        int year = shown.get(Calendar.YEAR);
        int month = shown.get(Calendar.MONTH);

        views.setTextViewText(R.id.txt_month,
                new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(shown.getTime()));

        Calendar first = (Calendar) shown.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int firstIdx = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Mon-first column of the 1st
        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar now = Calendar.getInstance();
        boolean showingNow = offset == 0;
        int today = now.get(Calendar.DAY_OF_MONTH);

        Set<Integer> eventDays = eventDays(context, year, month);

        for (int i = 0; i < 42; i++) {
            int dayId = resId(context, "day_" + i);
            int dotId = resId(context, "dot_" + i);
            int dayNum = i - firstIdx + 1;
            if (dayNum < 1 || dayNum > daysInMonth) {
                views.setTextViewText(dayId, "");
                views.setViewVisibility(dotId, android.view.View.GONE);
                views.setInt(dayId, "setBackgroundResource", 0);
                continue;
            }
            views.setTextViewText(dayId, String.valueOf(dayNum));
            boolean isToday = showingNow && dayNum == today;
            boolean isSunday = (i % 7) == 6;
            if (isToday) {
                views.setInt(dayId, "setBackgroundResource", R.drawable.today_pill);
                views.setTextColor(dayId, 0xFFFFFFFF);
            } else {
                views.setInt(dayId, "setBackgroundResource", 0);
                views.setTextColor(dayId, isSunday ? 0xFFF87171 : 0xFFE5E7EB);
            }
            views.setViewVisibility(dotId,
                    eventDays.contains(dayNum) ? android.view.View.VISIBLE : android.view.View.GONE);
        }

        // Month title: opens the calendar app, or asks for the calendar
        // permission first (needed for the event dots).
        boolean hasCalPerm = context.checkSelfPermission("android.permission.READ_CALENDAR")
                == PackageManager.PERMISSION_GRANTED;
        if (hasCalPerm) {
            Intent open = new Intent(Intent.ACTION_VIEW);
            open.setData(Uri.parse("content://com.android.calendar/time/"
                    + shown.getTimeInMillis()));
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            views.setOnClickPendingIntent(R.id.txt_month, PendingIntent.getActivity(
                    context, 40, open, flags()));
        } else {
            Intent ask = new Intent(context, CalPermActivity.class);
            ask.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            views.setOnClickPendingIntent(R.id.txt_month, PendingIntent.getActivity(
                    context, 41, ask, flags()));
        }

        // Quick Controls state
        boolean wifiOn = false;
        WifiManager wm = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            try { wifiOn = wm.isWifiEnabled(); } catch (Exception ignored) {}
        }
        views.setInt(R.id.tile_wifi, "setBackgroundResource",
                wifiOn ? R.drawable.tile_wifi_on : R.drawable.tile_off);
        views.setImageViewResource(R.id.img_wifi,
                wifiOn ? R.drawable.icon_wifi : R.drawable.icon_wifi_off);

        boolean btOn = false;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            try { btOn = adapter.isEnabled(); } catch (Exception ignored) {}
        }
        views.setInt(R.id.tile_bt, "setBackgroundResource",
                btOn ? R.drawable.tile_bt_on : R.drawable.tile_off);
        views.setImageViewResource(R.id.img_bt,
                btOn ? R.drawable.icon_bluetooth : R.drawable.icon_bluetooth_off);

        boolean torchOn = prefs.getBoolean(KEY_TORCH, false);
        views.setInt(R.id.tile_torch, "setBackgroundResource",
                torchOn ? R.drawable.tile_torch_on : R.drawable.tile_off);
        views.setImageViewResource(R.id.img_torch,
                torchOn ? R.drawable.icon_torch : R.drawable.icon_torch_off);

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            views.setProgressBar(R.id.bar_volume, 100,
                    percent(am, AudioManager.STREAM_MUSIC), false);
            views.setProgressBar(R.id.bar_ring, 100,
                    percent(am, AudioManager.STREAM_RING), false);
        }

        // Click wiring
        views.setOnClickPendingIntent(R.id.btn_prev, broadcast(context, ACTION_PREV, 1));
        views.setOnClickPendingIntent(R.id.btn_next, broadcast(context, ACTION_NEXT, 2));
        views.setOnClickPendingIntent(R.id.tile_wifi, broadcast(context, ACTION_TOGGLE_WIFI, 3));
        views.setOnClickPendingIntent(R.id.tile_bt, broadcast(context, ACTION_TOGGLE_BT, 4));
        views.setOnClickPendingIntent(R.id.tile_torch, broadcast(context, ACTION_TOGGLE_TORCH, 5));
        views.setOnClickPendingIntent(R.id.row_volume, sliderIntent(context, AudioManager.STREAM_MUSIC, 6));
        views.setOnClickPendingIntent(R.id.row_ring, sliderIntent(context, AudioManager.STREAM_RING, 7));
        return views;
    }

    private int percent(AudioManager am, int stream) {
        int max = am.getStreamMaxVolume(stream);
        if (max <= 0) return 0;
        return Math.round(am.getStreamVolume(stream) * 100f / max);
    }

    private int resId(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }

    private int flags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }

    private PendingIntent broadcast(Context context, String action, int code) {
        Intent intent = new Intent(context, BeautyCalendarWidget.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, code, intent, flags());
    }

    private PendingIntent sliderIntent(Context context, int stream, int code) {
        Intent intent = new Intent(context, SliderActivity.class);
        intent.putExtra(SliderActivity.EXTRA_STREAM, stream);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, code, intent, flags());
    }

    // -------------------------------------------------------------- events

    private Set<Integer> eventDays(Context context, int year, int month) {
        Set<Integer> days = new HashSet<>();
        if (context.checkSelfPermission("android.permission.READ_CALENDAR")
                != PackageManager.PERMISSION_GRANTED) {
            return days;
        }
        Calendar start = Calendar.getInstance();
        start.set(year, month, 1, 0, 0, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        Cursor cursor = null;
        try {
            // Instances.query(ContentResolver,long,long) is not in the
            // API 28 stub jar, so build the range URI the documented way.
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, start.getTimeInMillis());
            ContentUris.appendId(builder, end.getTimeInMillis());
            cursor = context.getContentResolver().query(builder.build(),
                    new String[]{CalendarContract.Instances.BEGIN,
                            CalendarContract.Instances.END},
                    null, null, null);
            if (cursor != null) {
                int beginCol = cursor.getColumnIndex(CalendarContract.Instances.BEGIN);
                int endCol = cursor.getColumnIndex(CalendarContract.Instances.END);
                Calendar walk = Calendar.getInstance();
                while (cursor.moveToNext()) {
                    long b = cursor.getLong(beginCol);
                    long e = Math.max(b, cursor.getLong(endCol) - 1);
                    walk.setTimeInMillis(b);
                    while (walk.getTimeInMillis() <= e) {
                        if (walk.get(Calendar.YEAR) == year
                                && walk.get(Calendar.MONTH) == month) {
                            days.add(walk.get(Calendar.DAY_OF_MONTH));
                        }
                        walk.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }
            }
        } catch (Exception ignored) {
            // Calendar provider unavailable or permission raced away: no dots.
        } finally {
            if (cursor != null) cursor.close();
        }
        return days;
    }

    // ------------------------------------------------------------- toggles

    private void toggleWifi(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                // Legal because the app targets API 28 (blocked at 29+).
                wm.setWifiEnabled(!wm.isWifiEnabled());
            }
        } catch (Exception ignored) {}
    }

    private void toggleBluetooth(Context context) {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                // Legal because the app targets API 28 (blocked at 33+).
                if (adapter.isEnabled()) {
                    adapter.disable();
                } else {
                    adapter.enable();
                }
            }
        } catch (Exception ignored) {}
    }

    private void toggleTorch(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean target = !prefs.getBoolean(KEY_TORCH, false);
        try {
            CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cm != null) {
                for (String id : cm.getCameraIdList()) {
                    Boolean hasFlash = cm.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (hasFlash != null && hasFlash) {
                        cm.setTorchMode(id, target);
                        prefs.edit().putBoolean(KEY_TORCH, target).apply();
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
