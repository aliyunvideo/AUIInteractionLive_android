package com.aliyun.aliinteraction.liveroom;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import com.aliyun.aliinteraction.InteractionService;
import com.aliyun.aliinteraction.common.biz.exposable.enums.LiveStatus;
import com.aliyun.aliinteraction.common.roombase.Const;
import com.aliyun.aliinteraction.core.event.EventListener;
import com.aliyun.aliinteraction.core.event.EventManager;
import com.aliyun.aliinteraction.liveroom.live.LiveLinkMicPushManager;
import com.aliyun.aliinteraction.liveroom.live.exposable.LivePlayerService;
import com.aliyun.aliinteraction.liveroom.live.exposable.LiveService;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;
import com.aliyun.aliinteraction.roompaas.message.AUIMessageService;

/**
 * @author puke
 * @version 2021/7/28
 */
public class BaseComponent implements IComponent, EventListener {

    protected LiveContext liveContext;
    protected EventManager eventManager;
    protected Activity activity;
    protected InteractionService interactionService;
    protected LiveService liveService;

    @CallSuper
    @Override
    public void onInit(LiveContext liveContext) {
        this.liveContext = liveContext;
        this.eventManager = liveContext.getEventManager();
        this.eventManager.register(this);
        this.activity = liveContext.getActivity();
        this.interactionService = liveContext.getInteractionService();
        this.liveService = liveContext.getLiveService();
    }

    @Override
    public void onEnterRoomSuccess(LiveModel liveModel) {

    }

    @Override
    public void onEnterRoomError(String errorMsg) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

    }

    @Override
    public void onActivityResume() {

    }

    @Override
    public void onActivityPause() {

    }

    @Override
    public void onActivityDestroy() {

    }

    @Override
    public void onActivityFinish() {

    }

    @Override
    public void onActivityConfigurationChanged(Configuration newConfig) {

    }

    @Override
    public boolean interceptBackKey() {
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void onEvent(String action, Object... args) {

    }

    /**
     * 发送事件
     *
     * @param action 事件标识
     * @param args   事件参数
     */
    public void postEvent(String action, Object... args) {
        eventManager.post(action, args);
    }

    public boolean isOwner() {
        return liveContext.getRole() == LivePrototype.Role.ANCHOR;
    }

    public String getGroupId() {
        return liveContext.getGroupId();
    }

    public String getLiveId() {
        return liveContext.getLiveId();
    }

    public String getAnchorId() {
        return liveContext.getLiveModel().anchorId;
    }

    public String getUserId() {
        return Const.getUserId();
    }

    public void showToast(String message) {
        Toast toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public boolean isLandscape() {
        if (liveContext == null) {
            return false;
        }
        return liveContext.isLandscape();
    }

    public void setLandscape(boolean landscape) {
        liveContext.setLandscape(landscape);
    }

    public AUIMessageService getMessageService() {
        return liveContext.getMessageService();
    }

    public LivePlayerService getPlayerService() {
        return liveContext.getLivePlayerService();
    }

    public LiveLinkMicPushManager getLiveLinkMicPushManager() {
        return liveContext.getLiveLinkMicPushManager();
    }

    public boolean supportLinkMic() {
        return liveContext.getLiveModel().mode != 0;
    }

    public LiveStatus getLiveStatus() {
        return liveContext.getLiveStatus();
    }

    @Nullable
    public String getPlaybackUrl() {
        return liveContext.getLiveModel().getPlaybackUrl();
    }

    public boolean needPlayback() {
        return !TextUtils.isEmpty(getPlaybackUrl()) && getLiveStatus() == LiveStatus.END;
    }
}
