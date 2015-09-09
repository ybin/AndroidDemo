package com.ybin.supportlibrarydemo;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * App的整体布局就是一个DrawerLayout，它分为两大部分：
     *      main frame:   内容展示区域
     *      drawer frame: 滑动抽屉区域
     * 为了使得drawer跟toolbar(action bar)实现联动，如点击toolbar icon会
     * 弹出、收缩drawer，手动关闭或者打开drawer(如左右滑动)时toolbar icon
     * 会有动画效果，即toolbar和drawer是有联动关系的，于是便创建了DrawerToggle
     * 类，它实现了DrawerListener接口，也是可以处理drawer的打开、关闭等事件，
     *      mDrawerLayout.setDrawerListener(mDrawerToggle);
     * 这样就把drawer跟drawer toggle关联起来了。其次还要把drawer toggle跟
     * toolbar关联起来，
     *      onOptionsItemSelected()中首先调用drawer toggle接口，
     *      if (mDrawerToggle.onOptionsItemSelected(item)) {
     *          return true;
     *      }
     * 这样toolbar的事件就传递给drawer toggle了，而它会将这些事件转换为
     * drawer的打开、关闭等动作。
     */
    private DrawerLayout mDrawerLayout;
    private CoordinatorLayout mRootContentView;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mRootContentView = (CoordinatorLayout) findViewById(R.id.main_frame);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.open_drawer_desc,
                R.string.close_drawer_desc);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        initInstance();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected() called with " + "item = [" + item.getItemId() + "]");

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initInstance() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(mRootContentView, "Hello, snackbar", Snackbar.LENGTH_SHORT)
                        .setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //
                            }
                        }).show();
            }
        });

        // tool bar
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // tab layout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Tab 1"));
        tabLayout.addTab(tabLayout.newTab().setText("Tab 2"));
        tabLayout.addTab(tabLayout.newTab().setText("Tab 3"));

//        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
//        collapsingToolbarLayout.setTitle("Design Library");
    }
}
