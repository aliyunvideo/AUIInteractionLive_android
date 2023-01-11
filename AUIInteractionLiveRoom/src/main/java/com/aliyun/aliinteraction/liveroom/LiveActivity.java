package com.aliyun.aliinteraction.liveroom;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;

import androidx.annotation.Nullable;

import com.alibaba.dingpaas.interaction.ImGetGroupStatisticsReq;
import com.alibaba.dingpaas.interaction.ImGetGroupStatisticsRsp;
import com.alibaba.dingpaas.interaction.ImJoinGroupReq;
import com.alibaba.dingpaas.interaction.ImJoinGroupRsp;
import com.aliyun.aliinteraction.InteractionEngine;
import com.aliyun.aliinteraction.InteractionService;
import com.aliyun.aliinteraction.base.Callback;
import com.aliyun.aliinteraction.base.Error;
import com.aliyun.aliinteraction.common.base.log.Logger;
import com.aliyun.aliinteraction.common.base.util.Utils;
import com.aliyun.aliinteraction.common.biz.exposable.enums.LiveStatus;
import com.aliyun.aliinteraction.common.roombase.Const;
import com.aliyun.aliinteraction.core.base.Actions;
import com.aliyun.aliinteraction.core.event.EventManager;
import com.aliyun.aliinteraction.enums.BroadcastType;
import com.aliyun.aliinteraction.liveroom.live.LiveLinkMicPushManager;
import com.aliyun.aliinteraction.liveroom.live.LivePlayerServiceImpl;
import com.aliyun.aliinteraction.liveroom.live.LiveServiceImpl;
import com.aliyun.aliinteraction.liveroom.live.exposable.LivePlayerService;
import com.aliyun.aliinteraction.liveroom.live.exposable.LiveService;
import com.aliyun.aliinteraction.liveroom.linkmic.AnchorPreviewHolder;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;
import com.aliyun.aliinteraction.roompaas.message.AUIMessageService;
import com.aliyun.aliinteraction.roompaas.message.AUIMessageServiceFactory;
import com.aliyun.aliinteraction.uikit.uibase.activity.BaseActivity;
import com.aliyun.aliinteraction.uikit.uibase.util.DialogUtil;

/**
 * 电商场景房间
 *
 * @author puke
 * @version 2021/5/11
 */
public class LiveActivity extends BaseActivity {

    private static final String TAG = LiveActivity.class.getSimpleName();

    private LivePrototype.Role role;
    private LiveStatus liveStatus;
    private String liveId;
    private LiveModel liveModel;
    private String userNick;
    private String userExtension;
    private String groupId;
    private String tips;

    private final ComponentManager componentManager = new ComponentManager();
    private final AnchorPreviewHolder anchorPreviewHolder = new AnchorPreviewHolder();
    private boolean isPushing = false;
    private AUIMessageService auiMessageService;
    private InteractionService interactionService;

    private LiveService liveService;
    private LivePlayerService livePlayerService;
    private LiveLinkMicPushManager pushManager;
    private LiveContext liveContext;

    private void parseParams(Intent intent) {
        LiveParam pageParam = (LiveParam) intent.getSerializableExtra(LiveConst.PARAM_KEY_LIVE_PARAM);
        liveId = pageParam.liveId;
        liveModel = pageParam.liveModel;
        role = pageParam.role;
        liveStatus = LiveStatus.of(liveModel.status);
        userNick = pageParam.userNick;
        userExtension = pageParam.userExtension;
        tips = pageParam.notice;

        groupId = liveModel.chatId;
        Logger.i(TAG, String.format("liveModel=%s", liveModel));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        parseParams(getIntent());
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Runnable asPermissionGrantedAction() {
        return new Runnable() {
            @Override
            public void run() {
                LiveActivity.this.init();
            }
        };
    }

    private void init() {
        // 获取RoomChannel
        InteractionEngine engine = InteractionEngine.instance();
        if (!engine.isLogin()) {
            Logger.e(TAG, "Not login");
            showToast("未登录");
            return;
        }

        pushManager = new LiveLinkMicPushManager(context, null);

        liveContext = new LiveContextImpl();
        setContentView(R.layout.ilr_activity_live);

        View decorView = getWindow().getDecorView();
        componentManager.scanComponent(decorView);
        componentManager.dispatchInit(liveContext);

        joiGroup(liveContext);
    }

    private void joiGroup(LiveContext liveContext) {
        ImJoinGroupReq joinGroupReq = new ImJoinGroupReq();
        joinGroupReq.groupId = groupId;
        joinGroupReq.userNick = userNick;
        joinGroupReq.userExtension = userExtension;
        joinGroupReq.broadCastType = BroadcastType.ALL.getValue();
        joinGroupReq.broadCastStatistics = true;
        liveContext.getInteractionService().joinGroup(joinGroupReq, new Callback<ImJoinGroupRsp>() {
            @Override
            public void onSuccess(ImJoinGroupRsp rsp) {
                if (isActivityValid()) {
                    onEnterRoomSuccess(liveModel);
                }
            }

            @Override
            public void onError(Error error) {
                if (isActivityValid()) {
                    onEnterRoomError(error.msg);

                    // 进入失败时, 退出房间
                    String message = String.format("进入房间失败：\n%s", error.msg);
                    DialogUtil.confirm(LiveActivity.this, message,
                            new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            },
                            new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            }
                    );
                }
            }
        });
    }

    @Override
    protected String[] parsePermissionArray() {
        if (role == LivePrototype.Role.ANCHOR) {
            // 主播
            return LiveConst.PERMISSIONS_4_ANCHOR;
        } else {
            if (liveModel.mode == 0) {//普通模式
                return LiveConst.PERMISSIONS_4_AUDIENCE;
            } else {
                // 互动模式
                return LiveConst.PERMISSIONS_4_AUDIENCE_OF_LINK_MIC;
            }
        }
    }

    @Override
    protected Runnable asPermissionGuidanceAction() {
        return new Runnable() {
            @Override
            public void run() {
                DialogUtil.showCustomDialog(LiveActivity.this,
                        "未开启拍摄权限，请在设置中允许使用拍摄和录音权限",
                        new Pair<CharSequence, Runnable>("设置权限", new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                                if (intent.resolveActivity(context.getPackageManager()) != null) {
                                    context.startActivity(intent);
                                }
                            }
                        }),
                        new Pair<CharSequence, Runnable>("取消", new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }));
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        componentManager.dispatchActivityConfigurationChanged(newConfig);
    }

    private void onEnterRoomSuccess(LiveModel liveModel) {
        this.liveModel = liveModel;
        componentManager.dispatchEnterRoomSuccess(liveModel);

        ImGetGroupStatisticsReq req = new ImGetGroupStatisticsReq();
        req.groupId = groupId;
        interactionService.getGroupStatistics(req, new Callback<ImGetGroupStatisticsRsp>() {
            @Override
            public void onSuccess(ImGetGroupStatisticsRsp rsp) {
                if (Utils.isActivityValid(LiveActivity.this)) {
                    componentManager.post(Actions.GET_GROUP_STATISTICS_SUCCESS, rsp);
                }
            }

            @Override
            public void onError(Error error) {

            }
        });
    }

    private void onEnterRoomError(String errorMsg) {
        componentManager.dispatchEnterRoomError(errorMsg);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        componentManager.dispatchActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        componentManager.dispatchActivityPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        componentManager.dispatchActivityResume();
    }

    @Override
    public void onBackPressed() {
        if (!componentManager.interceptBackKey()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        componentManager.dispatchActivityDestroy();
        liveContext.getMessageService().removeAllMessageListeners();
    }

    @Override
    public void finish() {
        super.finish();
        componentManager.dispatchActivityFinish();
    }

    private class LiveContextImpl implements LiveContext {

        @Override
        public Activity getActivity() {
            return LiveActivity.this;
        }

        @Override
        public LivePrototype.Role getRole() {
            return role;
        }

        @Override
        public String getNick() {
            return userNick;
        }

        @Override
        public String getTips() {
            return tips;
        }

        @Override
        public LiveStatus getLiveStatus() {
            return liveStatus;
        }

        @Override
        public EventManager getEventManager() {
            return componentManager;
        }

        @Override
        public boolean isPushing() {
            return isPushing;
        }

        @Override
        public void setPushing(boolean isPushing) {
            LiveActivity.this.isPushing = isPushing;
        }

        @Override
        public boolean isLandscape() {
            return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        }

        @Override
        public void setLandscape(boolean landscape) {
            if (landscape) {
                // 竖屏 => 横屏
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                // 横屏 => 竖屏
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        @Override
        public String getLiveId() {
            return liveId;
        }

        @Override
        public String getGroupId() {
            return groupId;
        }

        @Override
        public String getUserId() {
            return Const.getUserId();
        }

        @Override
        public LiveService getLiveService() {
            if (liveService == null) {
                liveService = new LiveServiceImpl(LiveActivity.this, liveContext);
            }
            return liveService;
        }

        @Override
        public LivePlayerService getLivePlayerService() {
            if (livePlayerService == null) {
                livePlayerService = new LivePlayerServiceImpl(LiveActivity.this);
            }
            return livePlayerService;
        }

        @Override
        public AUIMessageService getMessageService() {
            if (auiMessageService == null) {
                auiMessageService = AUIMessageServiceFactory.getMessageService(groupId);
            }
            return auiMessageService;
        }

        @Override
        public InteractionService getInteractionService() {
            if (interactionService == null) {
                interactionService = InteractionEngine.instance().getInteractionService();
            }
            return interactionService;
        }

        @Override
        public LiveLinkMicPushManager getLiveLinkMicPushManager() {
            return pushManager;
        }

        @Override
        public LiveModel getLiveModel() {
            return liveModel;
        }

        @Override
        public AnchorPreviewHolder getAnchorPreviewHolder() {
            return anchorPreviewHolder;
        }

        @Override
        public boolean isOwner(String userId) {
            if (liveModel != null) {
                String anchorId = liveModel.anchorId;
                if (!TextUtils.isEmpty(anchorId)) {
                    return TextUtils.equals(anchorId, userId);
                }
            }
            return false;
        }
    }
}
