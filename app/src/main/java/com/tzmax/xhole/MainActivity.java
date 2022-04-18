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

                StringBuilder stdOutSb = new StringBuilder();
                StringBuilder stdErrSb = new StringBuilder();


                try {
                    String cmdStr = "/bin/cat /proc/net/unix | /bin/grep _devtools_remote";
                    String[] cmd = new String[]{"sh", "-c" , cmdStr};
                    ShizukuRemoteProcess process = Shizuku.newProcess(cmd, null, null);

                    Thread stdOutD = IOUtils.writeStreamToStringBuilder(stdOutSb, process.getInputStream());
                    Thread stdErrD = IOUtils.writeStreamToStringBuilder(stdErrSb, process.getErrorStream());

                    process.waitFor();
                    stdOutD.join();
                    stdErrD.join();

                    Log.d(TAG, "退出: \n"+ process.exitValue());
                    Log.d(TAG, "失败: \n"+ stdErrSb.toString().trim());
                    Log.d(TAG, "成功: \n"+ stdOutSb.toString().trim());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        });
    }

    private void runLocalServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LocalSocket serverSocket = null;
                try {
//                    serverSocket = new LocalSocket();
                    LocalSocket client = new LocalSocket();
                    client.connect(new LocalSocketAddress("chrome_devtools_remote", LocalSocketAddress.Namespace.ABSTRACT));
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


}