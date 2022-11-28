package com.tzmax.xhole.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.tzmax.xhole.CompileActivity;
import com.tzmax.xhole.databinding.ItemScriptBinding;

import java.util.List;

public class ScriptListAdapter extends BaseAdapter {

    private Context mContext;
    private List<ScriptContent> data;

    public ScriptListAdapter(Context mContext, List<ScriptContent> data) {
        this.mContext = mContext;
        this.data = data;
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
        ScriptContent content = data.get(position);
        ItemScriptBinding binding = ItemScriptBinding.inflate(LayoutInflater.from(mContext));
        if (content == null || content.name == null || content.urlRule == null || content.scriptContent == null) {
            return new View(mContext);
        }

        binding.iScriptName.setText(content.name);
        binding.iScriptRule.setText(content.urlRule);
        if (content.author != null) {
            binding.iScriptAuthor.setText("@" + content.author);
        } else {
            binding.iScriptAuthor.setText("@本地用户");
        }


        binding.iScriptAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (content.website != null) {
                    Uri uri = Uri.parse(content.website);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    mContext.startActivity(intent);
                }
            }
        });


        binding.iScriptBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoCompile(position);
            }
        });

        return binding.getRoot();
    }

    // 跳转脚本编辑页面
    private void gotoCompile(int index) {
        Intent intent = new Intent(mContext, CompileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("index", index);
        mContext.startActivity(intent);
    }

}