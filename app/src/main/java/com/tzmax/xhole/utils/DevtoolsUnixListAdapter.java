package com.tzmax.xhole.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

// Devtools 调试 Unix 列表适配器
public class DevtoolsUnixListAdapter extends BaseAdapter {

    private List<String> data;
    private Context mContext;

    public DevtoolsUnixListAdapter(Context mContext, List<String> data) {
        this.data = data;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView != null) {
            parent.removeView(convertView);
        }

        TextView textView = new TextView(mContext);
        textView.setPadding(10, 10, 10, 10);
        textView.setText(data.get(position));

        return textView;
    }
}
