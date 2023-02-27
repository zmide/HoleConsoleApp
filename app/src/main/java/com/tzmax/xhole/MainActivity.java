package com.tzmax.xhole;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.system.ErrnoException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.ui.AppBarConfiguration;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tzmax.xhole.databinding.ActivityMainBinding;
import com.tzmax.xhole.databinding.ItemScriptBinding;
import com.tzmax.xhole.protocol.Command;
import com.tzmax.xhole.protocol.ParamsBean;
import com.tzmax.xhole.protocol.RuntimeEvaluate;
import com.tzmax.xhole.utils.DevtoolsInfoNode;
import com.tzmax.xhole.utils.DevtoolsUnixListAdapter;
import com.tzmax.xhole.utils.IOUtils;
import com.tzmax.xhole.utils.ScriptContent;
import com.tzmax.xhole.utils.ScriptListAdapter;
import com.tzmax.xhole.utils.Utils;

import java.io.IOException;
import java.io.*;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;
import rikka.sui.Sui;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "zmide";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;

    private Thread scanThread; // 调试服务扫描线程
    private boolean scanThreadState = false;

    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (binding == null) {
            return;
        }
        loadScriptList(); // 刷新脚本列表
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

                // connectADBServer();
                Thread taskRuntime = new Thread(() -> {
                    try {
                        List<String> list = getDevtoolsUnix();

                        runOnUiThread(() -> {
                            loadDevtoolsUnix(list);
                        });

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
                    } catch (Exception e) {
                    }
                });

                taskRuntime.start();

            }
        });

        binding.aMainStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取端口号
                String port = binding.aMainPort.getText().toString();
                if (port == "" || port.equals("")) {
                    toast("服务端口号不得为空");
                    return;
                }
                int portInt = Integer.parseInt(port);
                if (portInt <= 0) {
                    toast("服务端口号不在正确范围");
                    return;
                }

                startScanThread(portInt);
            }
        });

        // 绑定添加脚本按钮点击事件
        binding.aMainAddScript.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CompileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("index", -1);
                startActivity(intent);
            }
        });

    }

    // 加载脚本列表
    private void loadScriptList() {
        if (binding == null) {
            return;
        }

        List<ScriptContent> scriptContents = BaseApplication.application.localReadScriptContent();
        ScriptListAdapter adapter = new ScriptListAdapter(mContext, scriptContents);
        adapter.setScriptListEventNotice(new ScriptListAdapter.ScriptListEventNotice() {
            @Override
            public void onDeleteScript() {
                // 列表调用删除脚本后触发刷新数据
                loadScriptList();
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.aMainScriptList.setAdapter(adapter);
            }
        });
    }

    // 启动扫描服务
    private void startScanThread(int port) {
        String host = "127.0.0.1";
        if (scanThread != null) {
            // scanThread 扫描线程不为空，先停止线程然后清空线程
            scanThreadState = false; // 动态关闭线程
            try {
                scanThread.interrupt();
                scanThread.stop();
            } catch (Exception e) {
            }
            scanThread = null;
        }

        scanThreadState = true; // 动态打开线程
        scanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isSocketAlive = false;
                while (scanThreadState) {

                    while (!isSocketAlive) {
                        // 循环监听端口是否开放
                        isSocketAlive = isSocketAliveUtility(host, port);
                    }

                    // 端口开放，调用获取调试页面信息
                    try {
                        checkServiceInfo(host, port);
                    } catch (Exception e) {
                        // 获取页面调试信息错误，继续等待
                        Log.e(TAG, "run: 获取页面调试信息错误", e);
                    }

                }
            }
        });
        scanThread.start(); // 启动线程
    }

    // 查询服务信息
    OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(1, TimeUnit.SECONDS).readTimeout(1, TimeUnit.SECONDS).build();
    Gson gson = new Gson();
    String serviceInfoMD5Hash = "";

    private void checkServiceInfo(String host, int port) throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        Request request = new Request.Builder().url("http://" + host + ":" + port + "/json").method("GET", null).build();
        Response response = client.newCall(request).execute();
        String jsonStr = response.body().string();
        if (jsonStr.equals("")) {
            throw new IOException("checkServiceInfo is error: " + jsonStr);
        }

        // 判断调试页面信息是否有更新
        String jsonStrHash = Utils.md5(jsonStr);
        if (jsonStrHash.equals(serviceInfoMD5Hash)) {
            return;
        }
        serviceInfoMD5Hash = jsonStrHash;
        Log.d(TAG, "checkServiceInfo: " + jsonStr);

        Type type = new TypeToken<ArrayList<DevtoolsInfoNode>>() {
        }.getType();
        List<DevtoolsInfoNode> nodes = gson.fromJson(jsonStr, type);
        if (nodes.size() <= 0) {
            return;
        }

        // 开始匹配注入脚本
        matchInjectionScript(nodes);

    }

    private void matchInjectionScript(List<DevtoolsInfoNode> nodes) {
        // 遍历判断是否需要注入脚本
        List<ScriptContent> scriptContents = BaseApplication.application.localReadScriptContent();

        Log.d(TAG, "matchInjectionScript: 开始注入");
        for (DevtoolsInfoNode node : nodes) {
            // TODO:: 待实现

            Log.d(TAG, "matchInjectionScript: " + node.webSocketDebuggerUrl);

            // 遍历脚本列表
            for (ScriptContent script : scriptContents) {
                // 跳过异常的脚本
                if (script == null || script.urlRule == null || script.scriptContent == null) {
                    continue;
                }

                // 匹配脚本规则
                if (Utils.urlRoutingMatch(script.urlRule, node.url)) {
                    // 需要执行脚本
                    connectServiceExecutionScript(node, script);
                }
            }

            // 通过与 webSocketDebuggerUrl 建立连接发送命令

            // 1.先发送
            /*
            {
                "method": "Runtime.enable",
                    "params": {},
                "id": 1
            }
            */

            // 2.然后执行脚本命令
            /*
            {
                "method": "Runtime.evaluate",
                    "params": {
                "expression": "$x('//*[@id=\"form1help\"]/div[2]/div/input[1]')[0].value = \"testwwww\"",
                        "objectGroup": "console",
                        "includeCommandLineAPI": true,
                        "silent": false,
                        "returnByValue": false,
                        "generatePreview": true,
                        "userGesture": true,
                        "awaitPromise": false,
                        "replMode": true,
                        "allowUnsafeEvalBlockedByCSP": false,
                        "uniqueContextId": "1184210416429498045.4629085721122142521"
            },
                "id": 1
            }
            */

        }
    }

    // 连接服务并执行脚本
    private void connectServiceExecutionScript(DevtoolsInfoNode devtools, ScriptContent script) {
        OkHttpClient mClient = new OkHttpClient.Builder()
                .pingInterval(10, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(devtools.webSocketDebuggerUrl)
                .build();
        WebSocket mWebSocket = mClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
                Log.d(TAG, "webSocket onClosed");
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
                Log.d(TAG, "webSocket onClosing");
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                if (t != null) {
                    Log.e(TAG, "webSocket onFailure: ", t);
                    return;
                }
                Log.d(TAG, "webSocket onFailure: " + response.body());
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                Log.d(TAG, "webSocket onMessage: " + text);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                super.onMessage(webSocket, bytes);
            }

            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
                Log.d(TAG, "webSocket onOpen: " + response.body());

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.serializeNulls();
                Gson gson = gsonBuilder.create();

                // 连接成功时发送

                // 1. 发送 Runtime.enable 指令
                Command commandRuntimeEnable = new Command();
                commandRuntimeEnable.method = "Runtime.enable";
                commandRuntimeEnable.id = 1;
                commandRuntimeEnable.params = null;

                String msg = gson.toJson(commandRuntimeEnable);
                Log.d(TAG, "onOpen: " + msg);
                webSocket.send(msg);

                // 2. 发送 Runtime.evaluate 指令
                Command commandRuntimeEvaluate = new Command();
                commandRuntimeEvaluate.method = "Runtime.evaluate";
                commandRuntimeEvaluate.id = 2;
                commandRuntimeEvaluate.params = new RuntimeEvaluate(script.scriptContent);
                msg = gson.toJson(commandRuntimeEvaluate);
                Log.d(TAG, "onOpen: " + msg);
                webSocket.send(msg);

                // webSocket.cancel(); // 断开连接

                // 关闭连接池
                // mClient.dispatcher().executorService().shutdown();
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

    // 检查端口是否开放
    public static boolean isSocketAliveUtility(String hostName, int port) {
        boolean isAlive = false;
        SocketAddress socketAddress = new InetSocketAddress(hostName, port);
        Socket socket = new Socket();

        int timeout = 1000; // 超时时间
        try {
            socket.connect(socketAddress, timeout);
            socket.close();
            isAlive = true;
        } catch (SocketTimeoutException exception) {
            // 超时
            Log.d(TAG, "isSocketAliveUtility: 超时" + exception.getMessage());
            isAlive = false;
        } catch (IOException exception) {
            // 其他异常
            Log.d(TAG, "isSocketAliveUtility: 失败" + exception.getMessage());
            isAlive = false;
        }
        return isAlive;
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

    // 加载  devtools unix domain 列表
    private void loadDevtoolsUnix(List<String> data) {
        if (binding == null) {
            return;
        }

        DevtoolsUnixListAdapter adapter = new DevtoolsUnixListAdapter(mContext, data);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.aMainDevtoolsList.setAdapter(adapter);
                binding.aMainDevtoolsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData mClipData = ClipData.newPlainText("shell", data.get(position));
                        cm.setPrimaryClip(mClipData);
                        toast("指令已复制。");
                    }
                });

                binding.aMainDevtoolsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        // TODO:: 实现长按可以弹窗自定义需要复制的命令
                        toast("自定义命令功能正在开发。");
                        return true;
                    }
                });
            }
        });

        toast("devtools unix 列表加载完成。");
    }

    private CMDResult runCmd(String cmdStr) {
        // cmd app path 可执行应用在 Android 系统中可保存路径: /data/local/tmp/
        CMDResult result = null;
        StringBuilder stdOutSb = new StringBuilder();
        StringBuilder stdErrSb = new StringBuilder();
        try {
            String[] cmd = new String[]{"sh", "-c", cmdStr};

            int isPermission = Shizuku.checkSelfPermission();
            if (isPermission != PackageManager.PERMISSION_GRANTED) {
                toast("未获取 Shizuku 授权");
                throw new ErrnoException("not permission", 404);
            }

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

        } catch (Exception e) {
            // e.printStackTrace();
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

    private void connectADBServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
//                StringBuffer callbackStr = shellExec("sh cd / && sh ls \n");
//                Log.d(TAG, "run: callbackStr" + callbackStr);
                Process p = null;
                try {
                    p = Runtime.getRuntime().exec("ls");
                    String data = null;
                    BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String error = null;
                    while ((error = ie.readLine()) != null && !error.equals("null")) {
                        data += error + "\n";
                    }
                    String line = null;
                    while ((line = in.readLine()) != null && !line.equals("null")) {
                        data += line + "\n";
                    }
                    Log.d(TAG, "run: 执行成功 " + line);
                } catch (IOException e) {
                    Log.e(TAG, "run: 执行失败", e);
                    e.printStackTrace();
                }

            }
        }).start();
    }

    // 执行 Shell
    public static StringBuffer shellExec(String cmd) {
        Runtime mRuntime = Runtime.getRuntime(); //执行命令的方法
        try {
            //Process中封装了返回的结果和执行错误的结果
            Process mProcess = mRuntime.exec(cmd); //加入参数
            //使用BufferReader缓冲各个字符，实现高效读取
            //InputStreamReader将执行命令后得到的字节流数据转化为字符流
            //mProcess.getInputStream()获取命令执行后的的字节流结果
            BufferedReader mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
            //实例化一个字符缓冲区
            StringBuffer mRespBuff = new StringBuffer();
            //实例化并初始化一个大小为1024的字符缓冲区，char类型
            char[] buff = new char[1024];
            int ch = 0;
            //read()方法读取内容到buff缓冲区中，大小为buff的大小，返回一个整型值，即内容的长度
            //如果长度不为null
            while ((ch = mReader.read(buff)) != -1) {
                //就将缓冲区buff的内容填进字符缓冲区
                mRespBuff.append(buff, 0, ch);
            }
            //结束缓冲
            mReader.close();
            Log.i("shell", "shellExec: " + mRespBuff);
            //弹出结果
//            Log.i("shell", "执行命令: " + cmd + "执行成功");
            return mRespBuff;

        } catch (IOException e) {
            // 异常处理
            // TODO Auto-generated catch block
            Log.e(TAG, "shellExec: ", e);
            e.printStackTrace();
        }
        return null;
    }

}