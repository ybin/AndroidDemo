package com.ybin.drawablemodule;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class DrawableActivity extends Activity {
    Toast mExitToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExitToast = Toast.makeText(this, "quit?", Toast.LENGTH_LONG);

        setContentView(R.layout.drawable_layout);

        demoLevelList();
        demoTransition();
        demoRoundedDrawable();
    }

    private void demoLevelList() {
        ImageView levelList = (ImageView) findViewById(R.id.circle_button);
        levelList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drawable d = ((ImageView) v).getDrawable();
                d.setLevel((d.getLevel() + 1) % 3);
            }
        });
    }

    private void demoTransition() {
        ImageView transition = (ImageView) findViewById(R.id.transition);
        ((TransitionDrawable)transition.getDrawable()).startTransition(500);
        transition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drawable d = ((ImageView) v).getDrawable();
                ((TransitionDrawable) d).startTransition(500);
            }
        });
    }

    private void demoRoundedDrawable() {
        ImageView iv = (ImageView) findViewById(R.id.rounded_drawable);
        Resources res = getResources();
        Bitmap src = BitmapFactory.decodeResource(res, R.drawable.java_generics);
        RoundedBitmapDrawable dr =
                RoundedBitmapDrawableFactory.create(res, src);
        dr.setCornerRadius(Math.max(src.getWidth(), src.getHeight()) / 2.0f);
        iv.setImageDrawable(dr);
    }

    @Override
    public void onBackPressed() {
        if (mExitToast.getView().getParent() == null) {
            mExitToast.show();
        } else {
            finish();
        }
    }
}
