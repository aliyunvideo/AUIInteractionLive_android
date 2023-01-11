package com.aliyun.aliinteraction.liveroom.live.exposable;

import androidx.annotation.Nullable;

import com.aliyun.aliinteraction.liveroom.live.LiveEvent;

import java.util.Map;

/**
 * @author puke
 * @version 2021/7/2
 */
public interface LiveEventHandler {

    /**
     * 推流事件回调
     *
     * @param event 事件
     */
    void onPusherEvent(LiveEvent event, @Nullable Map<String, Object> extras);
}
