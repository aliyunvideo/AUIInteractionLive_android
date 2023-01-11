package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aliyun.aliinteraction.common.biz.exposable.enums.LiveStatus;
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
 * 直播准备图层
 *
 * @author puke
 * @version 2021/7/30
 */
public class LiveReadyLayer extends RelativeLayout implements ComponentHolder {

    private final Component component = new Component();

    public LiveReadyLayer(@NonNull Context context) {
        this(context, null, 0);
    }

    public LiveReadyLayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveReadyLayer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

//        LiveHook liveHook = LivePrototype.getInstance().getLiveHook();
//        View startLiveView = null;
//        if (liveHook != null) {
//            ViewSlot readySlot = liveHook.getReadySlot();
//            if (readySlot != null) {
//                startLiveView = readySlot.createView(context);
//            }
//        }

        // 外部未设置时, 提供默认兜底的开始直播按钮
//        if (startLiveView == null) {
//            setBackgroundColor(Color.parseColor("#66000000"));
//            startLiveView = new LiveStartView(context);
//            LayoutParams layoutParams = new LayoutParams(
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT
//            );
//            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
//            startLiveView.setLayoutParams(layoutParams);
//        }

//        addView(startLiveView);
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
                // 观众身份 或者 观看直播回放, 都不显示该图层
                setVisibility(GONE);
                return;
            }

            // 以下是主播的开播逻辑
            setVisibility(VISIBLE);
            liveService.addEventHandler(new SimpleLiveEventHandler() {
                @Override
                public void onPusherEvent(LiveEvent event, @Nullable Map<String, Object> extras) {
                    if (event == LiveEvent.PUSH_STARTED) {
                        setVisibility(View.GONE);
                    }
                }
            });

            getMessageService().addMessageListener(new SimpleOnMessageListener() {
                @Override
                public void onStartLive(Message<StartLiveModel> message) {
                    setVisibility(GONE);
                }
            });
        }
    }
}
