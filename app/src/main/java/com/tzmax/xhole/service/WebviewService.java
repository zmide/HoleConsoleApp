package com.tzmax.xhole.service;

import android.os.RemoteException;
import android.util.Log;

import com.tzmax.xhole.IWebview;

import java.io.IOException;

public class WebviewService extends IWebview.Stub {

    public WebviewService() {
    }

    @Override
    public void destroy() {
        Log.i("UserService", "destroy");
        System.exit(0);
    }

    @Override
    public void exit() {
        destroy();
    }

    @Override
    public String doSomething() throws RemoteException {
        return "";
    }

    static {
        try {
            Runtime.getRuntime().exec("cat /proc/net/unix | grep --text  _devtools_remote");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.loadLibrary("hello-jni");
    }

    public static native String stringFromJNI();
}
