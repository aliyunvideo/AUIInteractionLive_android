package com.aliyun.aliinteraction.common.base;

public class BaseSettings {
    protected static String sAppId;

    public static void setAppId(String appId) {
        sAppId = appId;
    }

    public static String getAppId() {
        return sAppId;
    }
}
