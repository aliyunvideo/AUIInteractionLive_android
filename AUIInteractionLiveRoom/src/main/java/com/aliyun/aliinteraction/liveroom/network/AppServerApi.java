package com.aliyun.aliinteraction.liveroom.network;

/**
 * @author puke
 * @version 2022/9/6
 */
public abstract class AppServerApi implements ApiService {

    public static ApiService instance() {
        return RetrofitManager.getService(ApiService.class);
    }
}
