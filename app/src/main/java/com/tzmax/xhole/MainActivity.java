package com.tzmax.xhole;

import android.content.pm.PackageManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.common.io.ByteStreams;
import com.tzmax.xhole.databinding.ActivityMainBinding;
import com.tzmax.xhole.utils.IOUtils;
import com.tzmax.xhole.utils.Utils;

import java.io.IOException;
import java.io.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import moe.shizuku.server.IRemoteProcess;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;
import rikka.sui.Sui;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "zmide";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
    }

    private void initView() {
        if (binding == null) {
            return;
        }

        Sui.init(BuildConfig.APPLICATION_ID);
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);

        Button mGetList = binding.aMainGetlist;
        mGetList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: ");
                // runLocalServer();

                List<String> list = getDevtoolsUnix();

                for (String item : list) {
                    Log.d(TAG, "getDevtoolsUnix item: " + item);
                    // forwardUnixServer(item);

                }

                if (list.size() > 0) {
                    Log.d(TAG, "onClick: 开始连接");

                    String server_api = forwardUnixServer(list.get(0)); // 转发调试连接
                    Log.d(TAG, "Server API:" + server_api);
                    // runLocalServer(list.get(0));
                }
            }
        });
    }

    // 启动转发
    private String forwardUnixServer(String domain, String port) {
        return "http://127.0.0.1:" + port;
    }

    private String forwardUnixServer(String domain) {
        String port = "5960";
        String cmdStr = "/data/local/tmp/adb forward tcp:" + port + " localabstract:chrome_devtools_remote";
        CMDResult result = runCmd(cmdStr);

        String outStr = result.getStdOut();
        if (outStr != null) {
            Log.d(TAG, "forwardUnixServer: " + outStr);
            return forwardUnixServer(domain, port);
        }
        return null;
    }

    // 获取 devtools unix domain 列表
    private List<String> getDevtoolsUnix() {

        List<String> devtools = new ArrayList<>();

        String cmdStr = "/bin/cat /proc/net/unix | /bin/grep _devtools_remote";
        CMDResult result = runCmd(cmdStr);

        String outStr = result.getStdOut();
        if (outStr != null) {
            Pattern r = Pattern.compile("@webview_devtools_remote_(\\d*)");
            Matcher m = r.matcher(outStr);
            int matcher_start = 0;
            while (m.find(matcher_start)) {
                String id = m.group(1);
                devtools.add("webview_devtools_remote_" + id);
                matcher_start = m.end();
            }
        }

        return devtools;
    }

    private CMDResult runCmd(String cmdStr) {
        // cmd app path 可执行应用在 Android 系统中可保存路径: /data/local/tmp/
        CMDResult result = null;
        StringBuilder stdOutSb = new StringBuilder();
        StringBuilder stdErrSb = new StringBuilder();
        try {
            String[] cmd = new String[]{"sh", "-c", cmdStr};
            ShizukuRemoteProcess process = Shizuku.newProcess(cmd, null, null);

            Thread stdOutD = IOUtils.writeStreamToStringBuilder(stdOutSb, process.getInputStream());
            Thread stdErrD = IOUtils.writeStreamToStringBuilder(stdErrSb, process.getErrorStream());

            process.waitFor();
            stdOutD.join();
            stdErrD.join();

            Log.d(TAG, "退出: \n" + process.exitValue());
            Log.d(TAG, "失败: \n" + stdErrSb.toString().trim());
            Log.d(TAG, "成功: \n" + stdOutSb.toString().trim());

            int code = process.exitValue(); // 终端退出代码
            String errStr = stdOutSb.toString().trim(); // 错误
            String outStr = stdOutSb.toString().trim(); // 标准输出

            result = new CMDResult(code, errStr, outStr);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void runLocalServer(String domain) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LocalSocket serverSocket = null;
                try {
//                    serverSocket = new LocalSocket();
                    LocalSocket client = new LocalSocket();
                    client.connect(new LocalSocketAddress(domain, LocalSocketAddress.Namespace.ABSTRACT));
                    Log.d(TAG, "run: 连接成功");
                    while (true) {
                        if (!client.isConnected()) {
                            Log.d(TAG, "run: 连接关闭");
                            return;
                        }
                        InputStream inputStream = client.getInputStream();

                        String msg = Utils.convertStreamToString(inputStream);
                        // String msg = new String(ByteStreams.toByteArray(inputStream ));
                        Log.d(TAG, "serverSocket recv =" + msg);

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: ", e);
                }
            }
        }).start();
    }

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
        // Do stuff based on the result and the request code
    }

    public class CMDResult {
        private int code;
        private String stdErr, stdOut;

        public CMDResult(int code, String stdErr, String stdOut) {
            this.code = code;
            this.stdErr = stdErr;
            this.stdOut = stdOut;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getStdErr() {
            return stdErr;
        }

        public void setStdErr(String stdErr) {
            this.stdErr = stdErr;
        }

        public String getStdOut() {
            return stdOut;
        }

        public void setStdOut(String stdOut) {
            this.stdOut = stdOut;
        }
    }

}