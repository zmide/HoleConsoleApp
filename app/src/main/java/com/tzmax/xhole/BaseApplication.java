package com.tzmax.xhole;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.tzmax.xhole.utils.ScriptContent;
import com.tzmax.xhole.utils.Utils;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import rikka.sui.Sui;

public class BaseApplication extends Application {
    private static boolean isSui;
    private String TAG = "zmide";
    private Gson gson = new Gson();
    public static BaseApplication application;


    public static boolean isSui() {
        return isSui;
    }

    static {
        isSui = Sui.init(BuildConfig.APPLICATION_ID);
        if (!isSui) {
            // If this is a multi-process application
            //ShizukuProvider.enableMultiProcessSupport( /* is current process the same process of ShizukuProvider's */ );
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BaseApplication.application = this;

        // Log.d("ShizukuSample", getClass().getSimpleName() + " onCreate | Process=" + ApplicationUtils.getProcessName());

        if (!isSui) {
            // If this is a multi-process application
            //ShizukuProvider.requestBinderForNonProviderProcess(this);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("L");
        }
        // ApplicationUtils.setApplication(this);

        // Log.d("ShizukuSample", getClass().getSimpleName() + " attachBaseContext | Process=" + ApplicationUtils.getProcessName());
    }

    // 加载全部脚本
    public List<ScriptContent> localReadScriptContent() {
        SharedPreferences pref = getSharedPreferences(Utils.LOCAL_SAVE_DATA_TAG, MODE_PRIVATE);
        String scriptsListStr = pref.getString("script_list", "[]");
        Log.d(TAG, "localReadScriptContent: " + scriptsListStr);
        Type type = new TypeToken<ArrayList<ScriptContent>>() {
        }.getType();
        List<ScriptContent> scripts = gson.fromJson(scriptsListStr, type);
        if (scripts.size() <= 0) {
            return new ArrayList<>();
        }
        return scripts;
    }

    // 获取指定位置脚本
    public ScriptContent localReadScriptContent(int index) {
        List<ScriptContent> scriptContents = localReadScriptContent();
        if (scriptContents.size() <= 0 || index < 0 || index > scriptContents.size() - 1) {
            return null;
        }
        return scriptContents.get(index);
    }

    // 保存脚本内容
    public void localSaveScriptContent(int index, ScriptContent script) {
        List<ScriptContent> scriptContents = localReadScriptContent();

        if (script == null && index != -1) {
            // script 参数如果是 null 删除该脚本
            scriptContents.remove(index);
        } else if (index == -1 && script != null) {
            // 如果 index 为 -1 代表新增脚本
            scriptContents.add(script);
        } else if (index > 0 && index <= scriptContents.size()) {
            // 更新脚本
            scriptContents.set(index, script);
        } else {
            // 参数不合规
            return;
        }

        Type listType = new TypeToken<List<ScriptContent>>() {
        }.getType();
        String jsonStr = gson.toJson(scriptContents, listType);
        Log.d(TAG, "localSaveScriptContent: " + jsonStr);

        // 保存数据到 SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences(Utils.LOCAL_SAVE_DATA_TAG, MODE_PRIVATE).edit();
        editor.putString("script_list", jsonStr);
        editor.apply();
    }

}
