// IWebview.aidl
package com.tzmax.xhole;

// Declare any non-default types here with import statements

interface IWebview {
    void destroy() = 16777114; // Destroy method defined by Shizuku server

    void exit() = 1; // Exit method defined by user

    String doSomething() = 2;
}