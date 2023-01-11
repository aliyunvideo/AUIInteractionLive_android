package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.aliyun.aliinteraction.liveroom.live.SimpleLivePlayerEventHandler;
import com.aliyun.aliinteraction.liveroom.BaseComponent;
import com.aliyun.aliinteraction.liveroom.ComponentHolder;
import com.aliyun.aliinteraction.liveroom.IComponent;
import com.aliyun.aliinteraction.liveroom.LiveContext;
import com.aliyun.aliinteraction.liveroom.R;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;
import com.aliyun.aliinteraction.model.Message;
import com.aliyun.aliinteraction.roompaas.message.listener.SimpleOnMessageListener;
import com.aliyun.aliinteraction.roompaas.message.model.StartLiveModel;
import com.aliyun.aliinteraction.roompaas.message.model.StopLiveModel;
import com.aliyun.aliinteraction.uikit.uibase.util.AnimUtil;

/**
 * @author puke
 * @version 2021/7/29
 */
public class LiveCurtainView extends View implements ComponentHolder {

    private final Component component = new Component();

    public LiveCurtainView(Context context) {
        this(context, null, 0);
    }

    public LiveCurtainView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveCurtainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundResource(R.drawable.ilr_live_bg_alic);
    }

    private void showCurtain() {
        AnimUtil.animIn(LiveCurtainView.this);
    }

    private void hideCurtain() {
        AnimUtil.animOut(LiveCurtainView.this);
    }

    @Override
    public IComponent getComponent() {
        return component;
    }

    private class Component extends BaseComponent {
        @Override
        public void onInit(LiveContext liveContext) {
            super.onInit(liveContext);
            getMessageService().addMessageListener(new SimpleOnMessageListener() {
                @Override
                public void onStartLive(Message<StartLiveModel> message) {
                    hideCurtain();
                }

                @Override
                public void onStopLive(Message<StopLiveModel> message) {
                    if (!isOwner()) {
                        showCurtain();
                    }
                }
            });

            getPlayerService().addEventHandler(new SimpleLivePlayerEventHandler() {
                @Override
                public void onRenderStart() {
                    hideCurtain();
                }

                @Override
                public void onPlayerError() {
                    showCurtain();
                }
            });
        }

        @Override
        public void onEnterRoomSuccess(LiveModel liveModel) {
            if (isOwner()) {
                hideCurtain();
            }
        }
    }
}
