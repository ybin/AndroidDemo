package com.ybin.testmodule;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class SimpleShape extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.simple_shape);

        ImageView levelList = (ImageView) findViewById(R.id.circle_button);
        levelList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drawable d = ((ImageView)v).getDrawable();
                d.setLevel((d.getLevel() + 1) % 3);
            }
        });

        ImageView transition = (ImageView) findViewById(R.id.transition);
        ((TransitionDrawable)transition.getDrawable()).startTransition(500);
        transition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drawable d = ((ImageView) v).getDrawable();
                ((TransitionDrawable)d).startTransition(500);
            }
        });
    }
}
