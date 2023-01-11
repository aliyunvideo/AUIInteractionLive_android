package com.aliyun.aliinteraction.liveroom.linkmic;

import com.alivc.live.player.annotations.AlivcLivePlayError;

public abstract class MultiInteractLivePushPullListener {

    public void onPullSuccess(String rtcPullUrl){}
    public void onPullError(String rtcPullUrl,AlivcLivePlayError errorType, String errorMsg){}
    public void onPullStop(String rtcPullUrl){}
    public void onPushSuccess(String rtcPullUrl){}
    public void onPushError(String rtcPullUrl){}
}
