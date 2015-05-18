package com.ybin.testmodule;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * All about text operations.
 */
public class TextActivity extends Activity {
    public static final String TAG = "TextActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_layout);

        spannableDemo();
        typefaceDemo();
        listFAIcons();
    }

    private void spannableDemo() {
        String str= "4s";
        SpannableString sstr = new SpannableString(str);

        sstr.setSpan(new RelativeSizeSpan(4f), 0, 1, 0); // set size
        sstr.setSpan(new ForegroundColorSpan(Color.RED), 0, 1, 0);// set color
        sstr.setSpan(new BackgroundColorSpan(Color.CYAN), 0, 1, 0);

        TextView tv;
        tv = (TextView) findViewById(R.id.the_text);
        tv.setText(sstr);
    }

    private void typefaceDemo() {
        Typeface font = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
        TextView icontext = (TextView) findViewById(R.id.webfont);
        icontext.setTypeface(font);
        icontext.setTextSize(40);
        icontext.setTextColor(getResources().getColorStateList(R.color.text_color_with_state));
    }

    private void listFAIcons() {
        int minCode = 0xf000;
        int maxCode = 0xf23a;

        List<Integer> ints = new ArrayList<>();
        for (int i = minCode; i <= maxCode; i++)
            ints.add(i);

        ListView lv = (ListView) findViewById(R.id.the_list);
        lv.setAdapter(new MyArrayAdapter(this, R.layout.list_item, ints));
    }

    class MyArrayAdapter extends ArrayAdapter<Integer> {
        private Context mContext;
        private Typeface mTypeface;

        MyArrayAdapter(Context c, int resId, List<Integer> integers) {
            super(c, resId, integers);
            mContext = c;
            mTypeface = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(mContext);
            }

            CharSequence s = Integer.toHexString(getItem(position))
                    + ":    " + Character.toChars(getItem(position))[0];
            SpannableString content = new SpannableString(s);
            content.setSpan(new RelativeSizeSpan(4f), s.length()-1, s.length(), 0);

            TextView tv = (TextView)convertView;
            tv.setTypeface(mTypeface);
            tv.setText(content);
            return tv;
        }
    }
}
