package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aliyun.aliinteraction.common.base.exposable.Callback;
import com.aliyun.aliinteraction.common.base.log.Logger;
import com.aliyun.aliinteraction.common.biz.exposable.enums.LiveStatus;
import com.aliyun.aliinteraction.core.base.Actions;
import com.aliyun.aliinteraction.liveroom.live.exposable.LivePlayerService;
import com.aliyun.aliinteraction.liveroom.live.exposable.LivePusherService;
import com.aliyun.aliinteraction.liveroom.BaseComponent;
import com.aliyun.aliinteraction.liveroom.ComponentHolder;
import com.aliyun.aliinteraction.liveroom.IComponent;
import com.aliyun.aliinteraction.liveroom.LiveContext;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;
import com.aliyun.aliinteraction.liveroom.network.ThreadUtil;
import com.aliyun.aliinteraction.model.Message;
import com.aliyun.aliinteraction.player.exposable.CanvasScale;
import com.aliyun.aliinteraction.roompaas.message.listener.SimpleOnMessageListener;
import com.aliyun.aliinteraction.roompaas.message.model.StartLiveModel;
import com.aliyun.aliinteraction.roompaas.message.model.StopLiveModel;
import com.aliyun.aliinteraction.uikit.uibase.util.ViewUtil;

/**
 * 媒体流渲染视图
 *
 * @author puke
 * @version 2021/7/30
 */
public class LiveRenderView extends FrameLayout implements ComponentHolder {

    private static final String TAG = LiveRenderView.class.getSimpleName();
    private static final int ORDER_RENDER = -100;

    protected View renderView;

    private final Component component = new Component();

    private boolean isConnectLinkMic = false;
    private boolean isLiveStopped = false;

//    private FrameLayout bigFL;
//
//    private FrameLayout smallFL;

    public LiveRenderView(@NonNull Context context) {
        this(context, null, 0);
    }

    public LiveRenderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveRenderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                component.postEvent(Actions.SHOW_LIVE_MENU);
            }
        });
    }

    @CanvasScale.Mode
    protected int parseScaleMode(boolean isAnchor) {
        return CanvasScale.Mode.ASPECT_FILL;
    }

    @Override
    public IComponent getComponent() {
        return component;
    }

    private class Component extends BaseComponent {

        @Override
        public void onInit(final LiveContext liveContext) {
            super.onInit(liveContext);

            getMessageService().addMessageListener(new SimpleOnMessageListener() {
                @Override
                public void onStartLive(Message<StartLiveModel> message) {
                    // 观众监听到直播开始时, 开始尝试拉流
                    if (!isOwner()) {
                        ThreadUtil.postUiDelay(2000, new Runnable() {
                            @Override
                            public void run() {//暂时delay，保证拉流可以拉到
                                tryPlayLive();
                            }
                        });
                    }
                }

                @Override
                public void onStopLive(Message<StopLiveModel> message) {
                    isLiveStopped = true;
                    if (!isOwner()) {
                        getPlayerService().stopPlay();
                    }
                }
            });
        }

        @Override
        public void onEnterRoomSuccess(LiveModel liveModel) {
            if (needPlayback()) {
                // 回放逻辑
                LivePlayerService playerService = getPlayerService();
                // 设置显示模式
                int contentMode = parseScaleMode(false);
                Logger.i(TAG, "tryPlayLive, contentMode=" + contentMode);
                playerService.setViewContentMode(contentMode);
                String playUrl = getPlaybackUrl();
                SurfaceView surfaceView = playerService.playUrl(playUrl);
                if (surfaceView.getParent() != LiveRenderView.this) {
                    ViewUtil.removeSelfSafely(surfaceView);
                    removeAllViews();
                    renderView = surfaceView;
                    addView(renderView);
                }
                return;
            }

            if (getLiveStatus() == LiveStatus.END) {
                // 已结束, 且不支持回放时, 不显示画面
                return;
            }

            // 未开始 或 进行中
            if (isOwner()) {
                // 主播
                startPreview();
            } else {
                // 观众, 开始观看
                if (liveService.getLiveModel().getLiveStatus() == LiveStatus.DOING) {
                    LivePusherService pusherService = liveService.getPusherService();
                    pusherService.startPreview(new Callback<View>() {
                        @Override
                        public void onSuccess(View view) {
                        }

                        @Override
                        public void onError(String errorMsg) {
                            showToast(errorMsg);
                        }
                    });
                    tryPlayLive();
                }
            }
        }

        private void startPreview() {
            LivePusherService pusherService = liveService.getPusherService();
            pusherService.setPreviewMode(parseScaleMode(true));

            if (supportLinkMic()) {
                FrameLayout previewContainer = new FrameLayout(getContext());
                pusherService.setRenderView(previewContainer, true);
            }

            //  pusherService.setRenderView(smallFL);
            pusherService.startPreview(new Callback<View>() {
                @Override
                public void onSuccess(View view) {
                    if (view != null) {
                        if (view.getParent() != LiveRenderView.this) {
                            ViewUtil.removeSelfSafely(view);
                            addView(view);

                            component.liveContext.getAnchorPreviewHolder().setPreviewView(view);
                        }
                    }
                }

                @Override
                public void onError(String errorMsg) {
                    showToast(errorMsg);
                }
            });
        }

        private void tryPlayLive() {
            LivePlayerService playerService = getPlayerService();
            // 设置显示模式
            int contentMode = parseScaleMode(false);
            Logger.i(TAG, "tryPlayLive, contentMode=" + contentMode);
            playerService.setViewContentMode(contentMode);
            String playUrl;
            LiveModel liveModel = liveService.getLiveModel();
            String cdnPullUrl = liveModel.getCdnPullUrl();
            if (!TextUtils.isEmpty(cdnPullUrl)) {
                playUrl = cdnPullUrl;
            } else {
                playUrl = liveModel.getPullUrl();
            }
            SurfaceView surfaceView = playerService.playUrl(playUrl);
            if (surfaceView.getParent() != LiveRenderView.this) {
                ViewUtil.removeSelfSafely(surfaceView);
                removeAllViews();
                renderView = surfaceView;
                addView(renderView);
            }
        }

        @Override
        public void onActivityConfigurationChanged(Configuration newConfig) {
            Logger.i(TAG, String.format("onActivityConfigurationChanged, isLandscape=%s", isLandscape()));
            updateFillModeForAudience();
        }

        private void updateFillModeForAudience() {

        }

        @Override
        public void onActivityDestroy() {
            removeAllViews();
        }

        @Override
        public int getOrder() {
            return ORDER_RENDER;
        }

        @Override
        public void onEvent(String action, Object... args) {
            switch (action) {
                case Actions.JOIN_LINK_MIC:
                    if (!isOwner()) {
                        isConnectLinkMic = true;
                        setVisibility(GONE);
                        getPlayerService().stopPlay();
                    }
                    break;
                case Actions.LEAVE_LINK_MIC:
                case Actions.KICK_LINK_MIC:
                    if (!isOwner() && isConnectLinkMic && !isLiveStopped) {
                        isConnectLinkMic = false;
                        setVisibility(VISIBLE);
                        tryPlayLive();
                    }
                    break;
            }
        }
    }
}
