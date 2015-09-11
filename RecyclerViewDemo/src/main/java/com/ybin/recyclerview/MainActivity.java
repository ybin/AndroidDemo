package com.ybin.recyclerview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.HORIZONTAL);
        adapter = new MyAdapter();
        RecyclerView.ItemDecoration decoration = new MyItemDecoration(this);

        adapter.setOnItemClickListener(new MyAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Snackbar.make(recyclerView, "pos: " + position, Snackbar.LENGTH_LONG).show();
            }
        });

//        recyclerView.addItemDecoration(decoration);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                adapter.addData(0);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                adapter.delData(0);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static class MyItemDecoration extends RecyclerView.ItemDecoration {
        private final Drawable divider;

        public MyItemDecoration(Context c) {
            divider = c.getResources().getDrawable(R.drawable.divider, c.getTheme());
        }
        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
//            final TypedArray a = parent.getContext().obtainStyledAttributes(new int[]{
//                    android.R.attr.listDivider
//            });
//            Drawable divider = a.getDrawable(0);
//            a.recycle();

            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);

                final int top = child.getBottom();
                final int bottom = top + divider.getIntrinsicHeight();
                divider.setBounds(left, top, right, bottom);
                divider.draw(c);
            }
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.set(0, 0, 0, divider.getIntrinsicHeight());
        }
    }

    private static class MyViewHolder extends RecyclerView.ViewHolder {

        public MyViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private ArrayList<String> mData = new ArrayList<>();
        private OnItemClickListener mOnItemClickListener;

        public MyAdapter() {
            for (int i = 0; i < 50; i++) {
                mData.add(i, "item " + i);
            }
        }

        interface OnItemClickListener {
            void onItemClick(View v, int position);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            mOnItemClickListener = listener;
        }

        public void addData(int pos) {
            mData.add(pos, "xxxx");
            notifyItemInserted(pos);
        }

        public void delData(int pos) {
            mData.remove(pos);
            notifyItemRemoved(pos);
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View t = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false);
            return new MyViewHolder(t);
        }

        @Override
        public void onBindViewHolder(MyViewHolder myViewHolder, final int position) {
            ((TextView) myViewHolder.itemView).setText(mData.get(position));
            myViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v, position);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        @Override
        public void onViewRecycled(MyViewHolder holder) {
        }

        @Override
        public int getItemViewType(int position) {
            return position % 2 == 0 ? 100 : 101;
        }


    }
}
