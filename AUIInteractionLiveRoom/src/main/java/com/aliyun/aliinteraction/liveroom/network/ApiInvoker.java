package com.aliyun.aliinteraction.liveroom.network;

import com.aliyun.aliinteraction.base.Callback;

/**
 * @author puke
 * @version 2022/8/31
 */
public interface ApiInvoker<T> {

    void invoke(Callback<T> callback);
}
