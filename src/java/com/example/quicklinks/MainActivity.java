package com.example.quicklinks;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;

// Welcome page (2026-10-25): shows the three widgets in the app's own
// theme, with short instructions. Pure display; all behavior lives in
// the widgets themselves. The manifest gives this activity a platform
// Material theme so nothing depends on the app theme's parent class.
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(Color.parseColor("#10121E"));
        getWindow().setNavigationBarColor(Color.parseColor("#10121E"));
    }
}
