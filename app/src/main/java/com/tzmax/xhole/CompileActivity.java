package com.tzmax.xhole;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tzmax.xhole.databinding.ActivityCompileBinding;
import com.tzmax.xhole.utils.DevtoolsInfoNode;
import com.tzmax.xhole.utils.ScriptContent;
import com.tzmax.xhole.utils.Utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CompileActivity extends AppCompatActivity {

    private ActivityCompileBinding binding;
    private Context mContext;

    private int scriptIndex = -1;
    private ScriptContent scriptObj;

    private String TAG = "zmide";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mContext = this;

        Intent intent = getIntent();
        scriptIndex = intent.getIntExtra("index", -1); // 获取脚本序号
        if (scriptIndex != -1) {
            scriptObj = BaseApplication.application.localReadScriptContent(scriptIndex);
        }
        if (scriptObj == null) {
            scriptObj = new ScriptContent();
        }

        // 支持导入逻辑
        if (scriptIndex == -1) {
            String importUrlRule = intent.getStringExtra("urlRule"), // 获取匹配规则
                    importScriptContent = intent.getStringExtra("scriptContent"); // 获取
            if (importUrlRule != null && !importUrlRule.equals("")) {
                scriptObj.urlRule = importUrlRule;
            }
            if (importScriptContent != null && !importScriptContent.equals("")) {
                scriptObj.scriptContent = importScriptContent;
            }
        }

        initView();
    }

    @Override
    public void finish() {
        String rule = binding.aCompileDomainRule.getText().toString(), script = binding.aCompileScriptContent.getText().toString();
        if (scriptIndex == -1) {
            // 是新创建脚本，只需要判断规则和脚本不为空就提示用户需要保存
            if (!rule.equals("") && !script.equals("")) {
                showHintSaveDialog();
                return;
            }
        } else {
            // 是修改脚本，需要判断脚本内容是否有修改
            ScriptContent oldScript = BaseApplication.application.localReadScriptContent(scriptIndex);
            if (scriptObj != null &&
                    oldScript != null &&
                    oldScript.name.equals(scriptObj.name) &&
                    oldScript.urlRule.equals(scriptObj.urlRule) &&
                    oldScript.scriptContent.equals(scriptObj.scriptContent)
            ) {
                goBack();
                return;
            }

            showHintSaveDialog();
            return;
        }

        goBack();
    }

    // 弹出提示用户是否需要保存弹窗
    private void showHintSaveDialog() {
        AlertDialog dialog = new AlertDialog.Builder(mContext).create();
        dialog.setTitle("提示");
        dialog.setMessage("脚本内容已修改，是否需要保存？");
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showSaveDialog();
            }
        });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                goBack();
            }
        });
        dialog.show();
    }

    // 退出页面
    private void goBack() {
        super.finish();
    }

    // 保存脚本内容
    private void saveContent() {
        if (scriptObj.name == null || scriptObj.name.equals("")) {
            toast("脚本名称不得为空。");
            return;
        }

        if (scriptObj.urlRule == null || scriptObj.urlRule.equals("")) {
            toast("匹配规则不得为空。");
            return;
        }

        if (scriptObj.scriptContent == null || scriptObj.scriptContent.equals("")) {
            toast("脚本内容不得为空。");
            return;
        }

        BaseApplication.application.localSaveScriptContent(scriptIndex, scriptObj);
    }

    private void initView() {
        if (binding == null) {
            return;
        }

        // 赋值编辑框内容
        if (scriptIndex != -1 && scriptObj != null) {
            if (scriptObj.urlRule != null && !scriptObj.urlRule.equals("")) {
                binding.aCompileDomainRule.setText(scriptObj.urlRule);
            }
            if (scriptObj.scriptContent != null && !scriptObj.scriptContent.equals("")) {
                binding.aCompileScriptContent.setText(scriptObj.scriptContent);
            }
        }

        // 设置返回按钮点击事件
        binding.aCompileBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 测试域名匹配
        binding.aCompileMatchTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ruleStr = binding.aCompileDomainRule.getText().toString();
                if (ruleStr.equals("")) {
                    toast("域名匹配规则不得为空。");
                    return;
                }

                EditText mDomain = new EditText(mContext);
                mDomain.setHint("请输入测试 url");
                if (Utils.MatchTestURLCache != null) {
                    mDomain.setText(Utils.MatchTestURLCache);
                }

                AlertDialog dialog = new AlertDialog.Builder(mContext).create();
                dialog.setTitle("测试 URL 匹配规则");
                dialog.setMessage("匹配规则：" + ruleStr);
                dialog.setView(mDomain);
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "测试", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 匹配规则
                        String domainStr = mDomain.getText().toString();
                        if (domainStr.equals("")) {
                            toast("测试 URL 不得为空。");
                            return;
                        }

                        boolean isMatch = Utils.urlRoutingMatch(ruleStr, domainStr);
                        if (isMatch) {
                            toast(domainStr + " 匹配成功。");
                        } else {
                            toast(domainStr + "匹配失败。");
                        }

                        Utils.MatchTestURLCache = domainStr;
                    }
                });
            }
        });

        // 保存脚本点击事件
        binding.aCompileSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scriptObj.urlRule == null || scriptObj.urlRule.equals("")) {
                    toast("匹配规则不得为空。");
                    return;
                }

                if (scriptObj.scriptContent == null || scriptObj.scriptContent.equals("")) {
                    toast("脚本内容不得为空。");
                    return;
                }

                showSaveDialog();
            }
        });

        // 域名匹配规则编辑框编辑监听事件
        binding.aCompileDomainRule.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                scriptObj.urlRule = s.toString();
            }
        });

        // 脚本内容编辑框监听事件
        binding.aCompileScriptContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                scriptObj.scriptContent = s.toString();
            }
        });

    }

    // 显示编辑脚本名称弹窗
    private void showSaveDialog() {

        EditText mName = new EditText(mContext);
        mName.setHint("请输入脚本名称");
        if (scriptObj.name != null) {
            mName.setText(scriptObj.name);
        }

        AlertDialog dialog = new AlertDialog.Builder(mContext).create();
        dialog.setTitle("保存脚本");
        dialog.setMessage("脚本名称：");
        dialog.setView(mName);
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nameStr = mName.getText().toString();
                if (nameStr.equals("")) {
                    toast("脚本名称不得为空。");
                    return;
                }

                scriptObj.name = nameStr;
                saveContent();
                dialog.cancel();
                goBack();

            }
        });
    }

    private void toast(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


}