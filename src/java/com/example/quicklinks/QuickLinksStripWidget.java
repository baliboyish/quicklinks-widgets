package com.example.quicklinks;

// The 4x1 strip edition of the AI Assistant widget. All behavior (tap
// wiring for the five buttons) is inherited from QuickLinksWidget; the
// strip layout reuses the same button IDs, so only the layout differs.
public class QuickLinksStripWidget extends QuickLinksWidget {

    @Override
    protected int getLayoutRes() {
        return R.layout.widget_layout_strip;
    }
}
