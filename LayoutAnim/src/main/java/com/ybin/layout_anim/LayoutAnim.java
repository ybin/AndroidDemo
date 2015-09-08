package com.ybin.layout_anim;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

public class LayoutAnim extends Activity {

    private ViewGroup mTransitedLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.ybin.testmodule.R.layout.layout_transition);
        mTransitedLayout = (ViewGroup) findViewById(com.ybin.testmodule.R.id.transited_layout);

        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setAnimator(LayoutTransition.APPEARING, ObjectAnimator.ofFloat(null, "alpha", 0, 1));
        layoutTransition.setAnimator(LayoutTransition.DISAPPEARING, ObjectAnimator.ofFloat(null, "alpha", 1, 0));
        layoutTransition.setDuration(1000);
        mTransitedLayout.setLayoutTransition(layoutTransition);
    }

    public void clickme(View view) {
        for (int i = 0; i < mTransitedLayout.getChildCount(); i++) {
            View v = mTransitedLayout.getChildAt(i);
            v.setVisibility(v.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
        }
    }
}
