package com.ybin.layout_anim;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class AnimListViewActivity extends Activity {

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String[] strings = {
                "aaaaaaa", "aaaaaaa", "aaaaaaa", "aaaaaaa", "aaaaaaa", "aaaaaaa", "aaaaaaa"
        };
        mListView = new ListView(this);
        ListAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return strings.length;
            }

            @Override
            public Object getItem(int position) {
                return strings[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v;
                if (convertView != null) {
                    v = (TextView) convertView;
                } else {
                    v = new TextView(getBaseContext());
                }
                v.setText(strings[position]);
                v.setTextColor(Color.DKGRAY);
                v.setTextSize(40);
                return v;
            }
        };

        mListView.setAdapter(adapter);

        Animation anim = AnimationUtils.loadAnimation(this, com.ybin.testmodule.R.anim.list_view_layout_anim);
        LayoutAnimationController controller = new LayoutAnimationController(anim, .2f);
        mListView.setLayoutAnimation(controller);
        mListView.setLayoutAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Log.d("sybbb", "start");
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.d("sybbb", "end");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        setContentView(mListView);
    }

}
