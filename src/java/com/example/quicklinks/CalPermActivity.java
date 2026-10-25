package com.example.quicklinks;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

/**
 * One-job activity: ask for READ_CALENDAR (needed for the event dots in
 * the Beauty Calendar widget), then refresh the widget and disappear.
 * The widget's month title routes here only while the permission is
 * missing; once granted, the title opens the calendar app instead.
 */
public class CalPermActivity extends Activity {

    private static final int REQ_CALENDAR = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkSelfPermission("android.permission.READ_CALENDAR")
                == PackageManager.PERMISSION_GRANTED) {
            done();
        } else {
            requestPermissions(new String[]{"android.permission.READ_CALENDAR"}, REQ_CALENDAR);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        done();
    }

    private void done() {
        Intent refresh = new Intent(this, BeautyCalendarWidget.class);
        refresh.setAction(BeautyCalendarWidget.ACTION_REFRESH);
        sendBroadcast(refresh);
        finish();
    }
}
