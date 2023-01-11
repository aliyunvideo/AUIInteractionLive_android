package com.aliyun.aliinteraction.liveroom.linkmic;

import android.content.Context;

import com.alivc.live.annotations.AlivcLiveMode;
import com.alivc.live.player.AlivcLivePlayInfoListener;
import com.alivc.live.player.AlivcLivePlayerImpl;
import com.alivc.live.player.annotations.AlivcLivePlayError;

/**
 * AlivcLivePlayer 封装类，用于多人连麦互动
 */
public class MultiAlivcLivePlayer extends AlivcLivePlayerImpl implements AlivcLivePlayInfoListener{

    private MultiInteractLivePushPullListener mListener;
    private boolean mIsPlaying = false;
    private String mRtcPullUrl;

    public MultiAlivcLivePlayer(Context context, AlivcLiveMode mode) {
        super(context, mode);
        setPlayInfoListener(this);
    }

    public void setMultiInteractPlayInfoListener(MultiInteractLivePushPullListener listener){
        this.mListener = listener;
    }

    @Override
    public void onPlayStarted() {
        mIsPlaying = true;
        if(mListener != null){
            mListener.onPullSuccess(mRtcPullUrl);
        }
    }
    
    @Override
    public void onPlayStopped() {
        mIsPlaying = false;
        if(mListener != null){
            mListener.onPullStop(mRtcPullUrl);
        }
    }

    @Override
    public void onFirstVideoFrameDrawn() {
    }

    @Override
    public void onError(AlivcLivePlayError alivcLivePlayError, String s) {
        mIsPlaying = false;
        if(mListener != null){
            mListener.onPullError(mRtcPullUrl,alivcLivePlayError,s);
        }
    }

    public boolean isPulling(){
        return mIsPlaying;
    }

    public void setAudienceId(String rtcPullUrl) {
        this.mRtcPullUrl = rtcPullUrl;
    }

    public String getAudienceId(){
        return mRtcPullUrl;
    }
}
