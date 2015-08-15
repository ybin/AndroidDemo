package com.example.ybin.myapplication;

import android.animation.ValueAnimator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static float VISIBLE_WIDTH;
    private static float ITEM_WIDTH;
    private static float INTERVAL;

    private ViewGroup mListView;
    private ValueAnimator mAnimator;
    private float mTotalDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (LinearLayout) findViewById(R.id.list_view);

        VISIBLE_WIDTH = getResources().getDimensionPixelSize(R.dimen.item_block_size) / 2;
        ITEM_WIDTH = getResources().getDimensionPixelSize(R.dimen.item_block_size);
        INTERVAL = VISIBLE_WIDTH / 1.4f;
        mTotalDistance = VISIBLE_WIDTH + (mListView.getChildCount() - 1) * INTERVAL + 200;

        mAnimator = ValueAnimator.ofFloat(0f, mTotalDistance * 2);
        mAnimator.setDuration(2000);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float origValue = (Float) animation.getAnimatedValue();
                float value;
                if (origValue <= mTotalDistance) {
                    value = origValue;
                    for (int i = 0; i < mListView.getChildCount(); i++) {
                        mListView.getChildAt(i).setTranslationX(
                                -ITEM_WIDTH +
                                        Math.min(VISIBLE_WIDTH, Math.max(0, value - INTERVAL*i))
                        );
                    }
                } else {
                    value = mTotalDistance*2 - origValue;
                    for (int i = 0; i < mListView.getChildCount(); i++) {
                        mListView.getChildAt(i).setTranslationX(
                                -ITEM_WIDTH +
                                        Math.min(VISIBLE_WIDTH, Math.max(0, value - (mListView.getChildCount()-1-i)*INTERVAL))
                        );
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        reset();
        mListView.setVisibility(View.VISIBLE);
        mAnimator.start();
    }

    private void reset() {
        mListView.setVisibility(View.INVISIBLE);
        for (int i = 0; i < mListView.getChildCount(); i++) {
            mListView.getChildAt(i).setTranslationX(0);
        }
    }
}
