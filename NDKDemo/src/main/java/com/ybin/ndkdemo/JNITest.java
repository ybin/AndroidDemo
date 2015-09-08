package com.ybin.ndkdemo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class JNITest extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText(JNIInterface.hello());
        tv.setTextSize(50);
        setContentView(tv);
    }
}
