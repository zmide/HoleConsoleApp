package com.tzmax.xhole.protocol;


public class RuntimeEvaluate implements ParamsBean {
    public String expression;
    public String objectGroup;
    public Boolean includeCommandLineAPI;
    public Boolean silent;
    public Boolean returnByValue;
    public Boolean generatePreview;
    public Boolean userGesture;
    public Boolean awaitPromise;
    public Boolean replMode;
    public Boolean allowUnsafeEvalBlockedByCSP;
    // public String uniqueContextId;

    public RuntimeEvaluate(String expression, String objectGroup, Boolean includeCommandLineAPI, Boolean silent, Boolean returnByValue, Boolean generatePreview, Boolean userGesture, Boolean awaitPromise, Boolean replMode, Boolean allowUnsafeEvalBlockedByCSP) {
        this.expression = expression;
        this.objectGroup = objectGroup;
        this.includeCommandLineAPI = includeCommandLineAPI;
        this.silent = silent;
        this.returnByValue = returnByValue;
        this.generatePreview = generatePreview;
        this.userGesture = userGesture;
        this.awaitPromise = awaitPromise;
        this.replMode = replMode;
        this.allowUnsafeEvalBlockedByCSP = allowUnsafeEvalBlockedByCSP;
    }

    public RuntimeEvaluate(String expression) {
        this(expression, "console", true, false, false, true, true, false, true, false);
    }
}

/*
{
    "method": "Runtime.evaluate",
    "id": 1,
    "params": {
        "expression": "console.log("hello");",
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
    }
}
*/