package com.aliyun.aliinteraction.liveroom;

import android.app.Activity;

import com.aliyun.aliinteraction.InteractionService;
import com.aliyun.aliinteraction.common.biz.exposable.enums.LiveStatus;
import com.aliyun.aliinteraction.core.event.EventManager;
import com.aliyun.aliinteraction.liveroom.live.LiveLinkMicPushManager;
import com.aliyun.aliinteraction.liveroom.live.exposable.LivePlayerService;
import com.aliyun.aliinteraction.liveroom.live.exposable.LiveService;
import com.aliyun.aliinteraction.liveroom.linkmic.AnchorPreviewHolder;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;
import com.aliyun.aliinteraction.roompaas.message.AUIMessageService;

/**
 * @author puke
 * @version 2021/7/29
 */
public interface LiveContext {

    Activity getActivity();

    LivePrototype.Role getRole();

    String getNick();

    String getTips();

    LiveStatus getLiveStatus();

    EventManager getEventManager();

    boolean isPushing();

    void setPushing(boolean isPushing);

    /**
     * @return 判断当前是否是横屏
     */
    boolean isLandscape();

    /**
     * 设置是否横屏
     *
     * @param landscape true:横屏; false:竖屏;
     */
    void setLandscape(boolean landscape);

    String getLiveId();

    String getGroupId();

    String getUserId();

    LiveService getLiveService();

    LivePlayerService getLivePlayerService();

    AUIMessageService getMessageService();

    InteractionService getInteractionService();

    LiveLinkMicPushManager getLiveLinkMicPushManager();

    LiveModel getLiveModel();

    AnchorPreviewHolder getAnchorPreviewHolder();

    boolean isOwner(String userId);
}
