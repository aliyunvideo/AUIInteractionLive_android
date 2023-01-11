package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aliyun.aliinteraction.common.biz.exposable.enums.LiveStatus;
import com.aliyun.aliinteraction.core.base.Actions;
import com.aliyun.aliinteraction.liveroom.live.LiveEvent;
import com.aliyun.aliinteraction.liveroom.live.SimpleLiveEventHandler;
import com.aliyun.aliinteraction.liveroom.BaseComponent;
import com.aliyun.aliinteraction.liveroom.ComponentHolder;
import com.aliyun.aliinteraction.liveroom.IComponent;
import com.aliyun.aliinteraction.liveroom.LiveContext;
import com.aliyun.aliinteraction.model.Message;
import com.aliyun.aliinteraction.roompaas.message.listener.SimpleOnMessageListener;
import com.aliyun.aliinteraction.roompaas.message.model.StartLiveModel;

import java.util.Map;

/**
 * 直播内容图层
 *
 * @author puke
 * @version 2021/7/30
 */
public class LiveContentLayer extends RelativeLayout implements ComponentHolder {

    private final Component component = new Component();

    public LiveContentLayer(@NonNull Context context) {
        this(context, null, 0);
    }

    public LiveContentLayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveContentLayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClipChildren(false);
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (component.needPlayback()) {
                    setVisibility(INVISIBLE);
                }
            }
        });
    }

    @Override
    public IComponent getComponent() {
        return component;
    }

    private class Component extends BaseComponent {
        @Override
        public void onInit(LiveContext liveContext) {
            super.onInit(liveContext);
            if (!isOwner() || needPlayback() || getLiveStatus() == LiveStatus.END) {
                // 观众身份, 一直显示该图层
                setVisibility(VISIBLE);
                return;
            }


            // 以下是主播逻辑
            setVisibility(GONE);
            liveService.addEventHandler(new SimpleLiveEventHandler() {
                @Override
                public void onPusherEvent(LiveEvent event, @Nullable Map<String, Object> extras) {
                    if (event == LiveEvent.PUSH_STARTED) {
                        setVisibility(View.VISIBLE);
                    }
                }
            });

            getMessageService().addMessageListener(new SimpleOnMessageListener() {
                @Override
                public void onStartLive(Message<StartLiveModel> message) {
                    setVisibility(VISIBLE);
                }
            });
        }

        @Override
        public void onEvent(String action, Object... args) {
            switch (action) {
                case Actions.SHOW_LIVE_MENU:
                    setVisibility(VISIBLE);
                    break;
                default:
                    break;
            }
        }
    }
}
