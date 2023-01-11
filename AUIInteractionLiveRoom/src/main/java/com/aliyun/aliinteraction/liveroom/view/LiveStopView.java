package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.alibaba.dingpaas.interaction.ImLeaveGroupReq;
import com.aliyun.aliinteraction.common.biz.exposable.enums.LiveStatus;
import com.aliyun.aliinteraction.enums.BroadcastType;
import com.aliyun.aliinteraction.liveroom.BaseComponent;
import com.aliyun.aliinteraction.liveroom.ComponentHolder;
import com.aliyun.aliinteraction.liveroom.IComponent;
import com.aliyun.aliinteraction.liveroom.LiveContext;
import com.aliyun.aliinteraction.liveroom.R;
import com.aliyun.aliinteraction.liveroom.live.LiveEvent;
import com.aliyun.aliinteraction.liveroom.live.SimpleLiveEventHandler;
import com.aliyun.aliinteraction.liveroom.model.StopLiveRequest;
import com.aliyun.aliinteraction.liveroom.network.AppServerApi;
import com.aliyun.aliinteraction.uikit.uibase.util.DialogUtil;

import java.util.Map;

/**
 * @author puke
 * @version 2021/7/29
 */
public class LiveStopView extends AppCompatImageView implements ComponentHolder {

    private static final int ORDER_STOP = 100;
    private static final String TAG = LiveStopView.class.getSimpleName();

    private final Component component = new Component();

    public LiveStopView(@NonNull Context context) {
        this(context, null, 0);
    }

    public LiveStopView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveStopView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setImageResource(R.drawable.ilr_icon_close);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                component.handleCloseClick();
            }
        });
    }

    protected String getStopTips() {
        return "还有观众正在路上，确定要结束直播吗？";
    }

    @Override
    public IComponent getComponent() {
        return component;
    }

    private class Component extends BaseComponent {
        @Override
        public void onInit(final LiveContext liveContext) {
            super.onInit(liveContext);

            liveService.addEventHandler(new SimpleLiveEventHandler() {
                @Override
                public void onPusherEvent(LiveEvent event, @Nullable Map<String, Object> extras) {
                    if (event == LiveEvent.PUSH_STARTED) {
                        liveContext.setPushing(true);
                    }
                }
            });
        }

        private void handleCloseClick() {
            activity.onBackPressed();
        }

        @Override
        public boolean interceptBackKey() {
            readyStop();
            // 拦截所有back操作
            return true;
        }

        private void readyStop() {
            // 是主播身份, 且正在推流, 则需要弹窗
            if (isOwner()) {
                if (getLiveStatus() == LiveStatus.END) {
                    doStop(true);
                } else {
                    DialogUtil.showCustomDialog(getContext(), getStopTips(), new Runnable() {
                        @Override
                        public void run() {
                            doStop(true);
                        }
                    }, null);
                }
            } else {
                doStop(false);
            }
        }

        private void doStop(boolean stopLive) {
            if (stopLive) {
                // 媒体
                liveService.getPusherService().stopLive(null);
                // IMSDK
                getMessageService().stopLive(null);
                // AppServer
                StopLiveRequest request = new StopLiveRequest();
                request.id = liveContext.getLiveId();
                request.userId = getUserId();
                AppServerApi.instance().stopLive(request).invoke(null);
            }
            if (isOwner() && !needPlayback()) {
                liveService.getPusherService().destroy();
            } else {
                getPlayerService().destroy();
            }
            ImLeaveGroupReq req = new ImLeaveGroupReq();
            req.groupId = liveContext.getGroupId();
            // 离开房间时, 不扩散消息 (业务接入时, 可根据自身业务诉求来修改)
            req.broadCastType = BroadcastType.NONE.getValue();
            interactionService.leaveGroup(req, null);
            activity.finish();
        }

        @Override
        public int getOrder() {
            // 后置回调的优先级, 保证最后处理 interceptBackKey 事件
            return ORDER_STOP;
        }
    }
}
