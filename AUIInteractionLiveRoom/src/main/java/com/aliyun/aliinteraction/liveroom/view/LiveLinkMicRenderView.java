package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliyun.aliinteraction.base.Callback;
import com.aliyun.aliinteraction.base.Error;
import com.aliyun.aliinteraction.common.base.base.Function;
import com.aliyun.aliinteraction.common.base.util.CollectionUtil;
import com.aliyun.aliinteraction.core.base.Actions;
import com.aliyun.aliinteraction.liveroom.BaseComponent;
import com.aliyun.aliinteraction.liveroom.IComponent;
import com.aliyun.aliinteraction.liveroom.LiveContext;
import com.aliyun.aliinteraction.liveroom.MultiComponentHolder;
import com.aliyun.aliinteraction.liveroom.R;
import com.aliyun.aliinteraction.liveroom.linkmic.LinkMicManageController;
import com.aliyun.aliinteraction.liveroom.live.LiveEvent;
import com.aliyun.aliinteraction.liveroom.live.LiveLinkMicPushManager;
import com.aliyun.aliinteraction.liveroom.model.GetMeetingInfoRequest;
import com.aliyun.aliinteraction.liveroom.model.LinkMicItemModel;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;
import com.aliyun.aliinteraction.liveroom.model.MeetingInfo;
import com.aliyun.aliinteraction.liveroom.network.AppServerApi;
import com.aliyun.aliinteraction.model.Message;
import com.aliyun.aliinteraction.roompaas.message.listener.SimpleOnMessageListener;
import com.aliyun.aliinteraction.roompaas.message.model.CameraStatusUpdateModel;
import com.aliyun.aliinteraction.roompaas.message.model.HandleApplyJoinLinkMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.JoinLinkMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.KickUserFromLinkMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.LeaveLinkMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.MicStatusUpdateModel;
import com.aliyun.aliinteraction.roompaas.message.model.StartLiveModel;
import com.aliyun.aliinteraction.roompaas.message.model.StopLiveModel;
import com.aliyun.aliinteraction.uikit.uibase.helper.RecyclerViewHelper;
import com.aliyun.aliinteraction.uikit.uibase.util.AppUtil;
import com.aliyun.aliinteraction.uikit.uibase.util.DialogUtil;
import com.aliyun.aliinteraction.uikit.uibase.util.ViewUtil;
import com.aliyun.aliinteraction.util.ThreadUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author puke
 * @version 2022/9/29
 */
public class LiveLinkMicRenderView extends FrameLayout implements MultiComponentHolder {

    public static final Object PAYLOAD_REFRESH_MIC_STATUS = new Object();

    private static final int SPAN_COUNT = 6;

    private final Component component = new Component();

    //    private final FrameLayout bigContainer;
//    private final FrameLayout smallContainer;

    private FrameLayout previewContainer;

    private LiveModel liveModel;
    private String anchorId;
    private LiveLinkMicPushManager pushManager;
    private boolean isJoinedLinkMic;
    private boolean isApplying;

    private final LinkMicManageController linkMicManageController;
    private final RecyclerViewHelper<LinkMicItemModel> recyclerViewHelper;

    public LiveLinkMicRenderView(@NonNull Context context) {
        this(context, null, 0);
    }

    public LiveLinkMicRenderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveLinkMicRenderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        previewContainer = new FrameLayout(context);
        inflate(context, R.layout.ilr_view_live_link_mic, this);
        final RecyclerView recyclerView = findViewById(R.id.link_mic_container);
//        bigContainer = findViewById(R.id.big_container);
//        smallContainer = findViewById(R.id.small_container);

        final GridLayoutManager layoutManager = new GridLayoutManager(context, SPAN_COUNT);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int itemCount = recyclerViewHelper.getItemCount();
                if (itemCount <= 4) {
                    return 3;
                } else {
                    return 2;
                }
            }
        });
        recyclerView.setLayoutManager(layoutManager);

        recyclerViewHelper = RecyclerViewHelper.of(recyclerView, R.layout.ilr_item_link_mic,
                new RecyclerViewHelper.HolderRenderer<LinkMicItemModel>() {
                    @Override
                    public void render(RecyclerViewHelper<LinkMicItemModel> viewHelper, final RecyclerViewHelper.ViewHolder holder, LinkMicItemModel model, int position, int itemCount, List<Object> payloads) {
                        if (payloads.contains(PAYLOAD_REFRESH_MIC_STATUS)) {
                            // ???????????????????????????
                            holder.getView(R.id.item_mic).setSelected(model.micOpened);
                            return;
                        }

                        if (component.isOwner()) {
                            if (itemCount == 1) {
                                // ???????????????????????????, ????????????
                                return;
                            } else {
                                View previewView = component.getAnchorPreviewView();
                                ViewParent parent = previewView.getParent();
                                if (parent != null) {
                                    ((ViewGroup) parent).removeView(previewView);
                                }
                                previewContainer.removeAllViews();
                                previewContainer.addView(previewView);
                            }
                        }

                        // ????????????
                        MarginLayoutParams layoutParams = (MarginLayoutParams) holder.itemView.getLayoutParams();
                        int totalWidth = AppUtil.getScreenWidth() - 2 * getResources().getDimensionPixelOffset(R.dimen.ilr_link_mic_item_margin);
                        int spanSize = layoutManager.getSpanSizeLookup().getSpanSize(position);
                        final int itemSize = totalWidth * spanSize / SPAN_COUNT;

                        // ?????????????????????
                        layoutParams.width = itemSize;
                        layoutParams.height = itemSize;

                        // ??????????????????
                        int columns = SPAN_COUNT / spanSize;
                        boolean gridFilled = itemCount % columns == 0;
                        int secondLastRowValve = (int) Math.floor(itemCount * 1f / columns);
                        float actualRow = (position + 1f) / columns;
                        boolean isLastRow = actualRow > secondLastRowValve;
                        if (!gridFilled && isLastRow) {
                            int itemCountOfLastRow = itemCount % columns;
                            int totalWidthOfHolderView = itemCountOfLastRow * itemSize;
                            layoutParams.leftMargin = (totalWidth - totalWidthOfHolderView) / 2;
                        } else {
                            layoutParams.leftMargin = 0;
                        }

                        holder.itemView.setLayoutParams(layoutParams);

                        boolean isAnchor = TextUtils.equals(model.userId, anchorId);
                        holder.getView(R.id.item_anchor_flag).setVisibility(isAnchor ? VISIBLE : GONE);

                        FrameLayout container = holder.getView(R.id.item_container);
                        TextView nick = holder.getView(R.id.item_nick);
                        nick.setText(model.userNick);

                        holder.getView(R.id.item_mic).setSelected(model.micOpened);

                        boolean isSelf = TextUtils.equals(model.userId, component.getUserId());
                        if (isSelf) {
                            // ???????????????
                            ViewParent parent = previewContainer.getParent();
                            if (parent != container) {
                                if (parent != null) {
                                    ((ViewGroup) parent).removeView(previewContainer);
                                }
                                container.addView(previewContainer);
                            }
                        } else {
                            // ???????????????
                            String rtcPullUrl = model.rtcPullUrl;
                            if (TextUtils.isEmpty(rtcPullUrl)) {
                                container.removeAllViews();
                            } else {
                                pushManager.linkMic(container, rtcPullUrl);
                                // ??????key-player???????????????
                                pushManager.createAlivcLivePlayer(rtcPullUrl);
                            }
                        }
                    }
                }
        );

        linkMicManageController = new LinkMicManageController(context, new LinkMicManageController.Callback() {
            @Override
            public LinkMicItemModel getAnchorItemModel() {
                List<LinkMicItemModel> dataList = recyclerViewHelper.getDataList();
                int anchorIndex = CollectionUtil.findIndex(dataList, new Function<LinkMicItemModel, Boolean>() {
                    @Override
                    public Boolean apply(LinkMicItemModel model) {
                        return TextUtils.equals(model.userId, LiveLinkMicRenderView.this.anchorId);
                    }
                });
                return anchorIndex >= 0 ? dataList.get(anchorIndex) : null;
            }
        });
    }

    @Override
    public List<IComponent> getComponents() {
        return Arrays.asList(component, linkMicManageController.getComponent());
    }

    private class Component extends BaseComponent {

        @Override
        public void onInit(final LiveContext liveContext) {
            super.onInit(liveContext);
            if (!supportLinkMic()) {
                return;
            }

            liveModel = liveContext.getLiveModel();
            anchorId = liveModel.anchorId;
            pushManager = liveContext.getLiveLinkMicPushManager();
            pushManager.setCallback(new LiveLinkMicPushManager.Callback() {
                @Override
                public void onEvent(LiveEvent event, @Nullable Map<String, Object> extras) {
                    switch (event) {
                        case PUSH_STARTED:
                            // ?????? todo
                            // pushManager.linkMic(bigContainer, anchorPullUrl);
                            break;
                    }
                }
            });

            liveContext.getMessageService().addMessageListener(new SimpleOnMessageListener() {

                @Override
                public void onStartLive(Message<StartLiveModel> message) {
                    if (isOwner()) {
                        // ??????
                        pushManager.addAnchorMixTranscodingConfig(liveContext.getLiveModel().anchorId);
                    }
                }

                @Override
                public void onStopLive(Message<StopLiveModel> message) {
                    postEvent(Actions.LEAVE_LINK_MIC);
                }

                @Override
                public void onHandleApplyJoinLinkMic(final Message<HandleApplyJoinLinkMicModel> message) {
                    if (isOwner()) {
                        // ????????????????????????
                        return;
                    }

                    if (!isApplying) {
                        // ???????????????????????????, ????????????????????????, ?????????????????????
                        return;
                    }

                    HandleApplyJoinLinkMicModel model = message.data;
                    if (!model.agree) {
                        showToast("?????????????????????????????????");
                        return;
                    }

                    if (isJoinedLinkMic) {
                        // ????????????, ????????????
                        return;
                    }

                    DialogUtil.confirm(getContext(), "??????????????????????????????????????????",
                            new Runnable() {
                                @Override
                                public void run() {
                                    // ????????????
                                    GetMeetingInfoRequest request = new GetMeetingInfoRequest();
                                    request.id = getLiveId();
                                    AppServerApi.instance().getMeetingInfo(request).invoke(new Callback<MeetingInfo>() {
                                        @Override
                                        public void onSuccess(MeetingInfo meetingInfo) {
                                            List<LinkMicItemModel> members = meetingInfo == null ? null : meetingInfo.members;
                                            if (CollectionUtil.size(members) < LinkMicManageController.maxLinkMicLimit) {
                                                // ???????????????, ????????????????????????
                                                performJoinLinkMic(message);
                                            } else {
                                                // ????????????????????????, ????????????
                                                showToast("??????????????????????????????????????????");
                                                isApplying = false;
                                                postEvent(Actions.JOIN_BUT_MAX_LIMIT_WHEN_ANCHOR_AGREE);
                                            }
                                        }

                                        @Override
                                        public void onError(Error error) {
                                            showToast(error.msg);
                                            isApplying = false;
                                            postEvent(Actions.GET_MEMBERS_FAIL_WHEN_ANCHOR_AGREE);
                                        }
                                    });
                                }
                            },
                            new Runnable() {
                                @Override
                                public void run() {
                                    // ????????????
                                    isApplying = false;
                                    postEvent(Actions.REJECT_JOIN_LINK_MIC_WHEN_ANCHOR_AGREE);
                                }
                            }
                    );
                }

                private void performJoinLinkMic(Message<HandleApplyJoinLinkMicModel> message) {
                    // ??????????????????
                    List<LinkMicItemModel> initDataList = new ArrayList<>();

                    // ??????RecyclerView?????????????????????
                    pushManager.startPreview(previewContainer);
                    String myUserId = getUserId();
                    LinkMicItemModel selfItem = new LinkMicItemModel();
                    selfItem.userId = myUserId;
                    selfItem.userNick = liveContext.getNick();
                    selfItem.micOpened = true;
                    selfItem.cameraOpened = true;
                    initDataList.add(selfItem);

                    recyclerViewHelper.setData(initDataList);

                    updateVisible();

                    // ??????
                    pushManager.startPublish(liveModel.linkInfo.rtcPushUrl);
                    // ??????LiveRenderView??????cdn??????
                    postEvent(Actions.JOIN_LINK_MIC);
                    // ?????????
                    isApplying = false;

                    // todo ??????????????????????????????
                    ThreadUtil.postUiDelay(300, new Runnable() {
                        @Override
                        public void run() {
                            afterPushSuccess();
                        }
                    });
                }

                @Override
                public void onJoinLinkMic(final Message<JoinLinkMicModel> message) {
                    boolean isSelf = TextUtils.equals(message.senderId, getUserId());
//                    if (isSelf) {
//                        // ?????????????????????????????????
//                        return;
//                    }

                    if (isOwner()) {
                        // ??????
                        ThreadUtil.postUiDelay(500, new Runnable() {
                            @Override
                            public void run() {
                                updateMixStreamLayout();
                            }
                        });
                    } else {
                        // ??????
                        if (!isJoinedLinkMic) {
                            // ???????????????, ?????????
                            return;
                        }
                    }

                    // ????????????, ?????????????????????????????????
                    int indexOfAlreadyExistUserId = findIndexFromRecyclerView(message.senderId);
                    if (indexOfAlreadyExistUserId >= 0) {
                        recyclerViewHelper.removeData(indexOfAlreadyExistUserId);
                    }

                    // ???????????????????????????
                    LinkMicItemModel otherItem = new LinkMicItemModel();
                    otherItem.userId = message.senderId;
                    otherItem.userNick = message.senderInfo.userNick;
                    otherItem.userAvatar = message.senderInfo.userAvatar;
                    otherItem.rtcPullUrl = message.data.rtcPullUrl;
                    otherItem.micOpened = true;
                    otherItem.cameraOpened = true;
                    recyclerViewHelper.addDataWithoutAnimation(Collections.singletonList(otherItem));

                    updateVisible();
                }

                @Override
                public void onMicStatusUpdate(Message<MicStatusUpdateModel> message) {
                    if (isOwner() || isJoinedLinkMic) {
                        List<LinkMicItemModel> dataList = recyclerViewHelper.getDataList();
                        for (int i = 0; i < dataList.size(); i++) {
                            LinkMicItemModel model = dataList.get(i);
                            if (TextUtils.equals(model.userId, message.senderId)) {
                                // ??????????????????
                                model.micOpened = message.data.micOpened;
                                recyclerViewHelper.updateData(i, PAYLOAD_REFRESH_MIC_STATUS);
                            }
                        }
                    }
                }

                @Override
                public void onCameraStatusUpdate(Message<CameraStatusUpdateModel> message) {
                    if (isOwner() || isJoinedLinkMic) {
                        List<LinkMicItemModel> dataList = recyclerViewHelper.getDataList();
                        for (int i = 0; i < dataList.size(); i++) {
                            LinkMicItemModel model = dataList.get(i);
                            if (TextUtils.equals(model.userId, message.senderId)) {
                                model.cameraOpened = message.data.cameraOpened;
                                recyclerViewHelper.updateData(i);
                            }
                        }
                    }
                }

                @Override
                public void onLeaveLinkMic(final Message<LeaveLinkMicModel> message) {
                    // ???????????????????????????; ????????????;
                    if (isOwner() || isJoinedLinkMic) {
                        int leaveIndex = findIndexFromRecyclerView(message.senderId);
                        if (leaveIndex >= 0) {
                            // ???????????????????????????
                            LinkMicItemModel model = recyclerViewHelper.getDataList().get(leaveIndex);
                            getLiveLinkMicPushManager().stopPull(model.rtcPullUrl);
                            // ??????RecyclerView??????
                            recyclerViewHelper.removeDataWithoutAnimation(leaveIndex, 1);
                            updateVisible();
                        }
                    }

                    if (isOwner()) {
                        // ?????????????????????????????????
//                        pushManager.removeAudienceLiveMixTranscodingConfig(message.senderId, liveModel.anchorId);
                        ThreadUtil.postUiDelay(500, new Runnable() {
                            @Override
                            public void run() {
                                updateMixStreamLayout();
                            }
                        });
                    }
                }

                @Override
                public void onKickUserFromLinkMic(Message<KickUserFromLinkMicModel> message) {
                    if (isJoinedLinkMic) {
                        postEvent(Actions.KICK_LINK_MIC);
                    }
                }
            });
        }

        private int findIndexFromRecyclerView(String userId) {
            List<LinkMicItemModel> dataList = recyclerViewHelper.getDataList();
            return LinkMicItemModel.findIndexByUserId(dataList, userId);
        }

        // ??????????????????????????????
        private void updateVisible() {
            int itemCount = recyclerViewHelper.getItemCount();
            if (itemCount <= 0) {
                setVisibility(GONE);
                return;
            }

            if (isOwner()) {
                if (itemCount == 1) {
                    // ???????????????
                    setVisibility(GONE);
                    liveContext.getAnchorPreviewHolder().returnBigContainer();
                } else {
                    // ???????????????
                    setVisibility(VISIBLE);
                }
            } else {
                setVisibility(VISIBLE);
            }
        }

        private void returnAnchorPreviewViewIfNeed() {
//            previewContainer
        }

        // ??????????????????
        private void afterPushSuccess() {
            updateVisible();
            // 1. ??????????????????
            isJoinedLinkMic = true;

            // 2. ????????????, ???????????????
            getMessageService().joinLinkMic(liveModel.linkInfo.rtcPullUrl, null);

            // 3. ??????????????????????????????
            GetMeetingInfoRequest infoRequest = new GetMeetingInfoRequest();
            infoRequest.id = getLiveId();
            AppServerApi.instance().getMeetingInfo(infoRequest).invoke(new Callback<MeetingInfo>() {
                @Override
                public void onSuccess(MeetingInfo meetingInfo) {
                    List<LinkMicItemModel> members = meetingInfo.members;
                    if (CollectionUtil.isEmpty(members)) {
                        return;
                    }

                    List<LinkMicItemModel> holderModels = new ArrayList<>();
                    Set<String> userIds = new HashSet<>();
                    // ???????????????????????????
                    for (LinkMicItemModel model : members) {
                        String userId = model.userId;
                        if (userIds.contains(userId)) {
                            // ??????
                            continue;
                        }
                        if (isOwner() && TextUtils.equals(userId, anchorId)) {
                            // ??????????????????????????????????????????
                            continue;
                        }
                        holderModels.add(model);
                        userIds.add(userId);
                    }
                    // ????????????????????????
                    List<LinkMicItemModel> dataList = recyclerViewHelper.getDataList();
                    for (LinkMicItemModel model : dataList) {
                        String userId = model.userId;
                        if (userIds.contains(userId)) {
                            // ??????
                            continue;
                        }
                        holderModels.add(model);
                        userIds.add(userId);
                    }
                    // ??????: ?????????????????????
                    Collections.sort(holderModels, new Comparator<LinkMicItemModel>() {
                        @Override
                        public int compare(LinkMicItemModel o1, LinkMicItemModel o2) {
                            boolean isAnchor1 = TextUtils.equals(o1.userId, anchorId);
                            boolean isAnchor2 = TextUtils.equals(o2.userId, anchorId);
                            if (isAnchor1 ^ isAnchor2) {
                                return isAnchor1 ? -1 : 1;
                            }
                            return 0;
                        }
                    });
                    recyclerViewHelper.setData(holderModels);
                }

                @Override
                public void onError(Error error) {
                    showToast("??????????????????????????????");
                }
            });
        }

        @Override
        public void onEvent(String action, Object... args) {
            switch (action) {
                case Actions.APPLY_JOIN_LINK_MIC:
                    isApplying = true;
                    break;
                case Actions.CANCEL_APPLY_JOIN_LINK_MIC:
                    isApplying = false;
                    break;
                case Actions.LEAVE_LINK_MIC:
                case Actions.KICK_LINK_MIC:
                    // ??????
                    if (Actions.KICK_LINK_MIC.equals(action)) {
                        showToast("?????????????????????");
                    }
                    // ?????????
                    pushManager.stopPublish();
                    // ????????????
                    previewContainer.removeAllViews();
                    ViewUtil.removeSelfSafely(previewContainer);
                    // ?????????
                    pushManager.stopPull();
                    // ?????????
                    isJoinedLinkMic = false;
                    isApplying = false;
                    // ????????????
                    recyclerViewHelper.removeAll();
                    // ???????????????????????????
                    String reason = Actions.LEAVE_LINK_MIC.equals(action)
                            ? LeaveLinkMicModel.REASON_BY_SELF : LeaveLinkMicModel.REASON_BY_KICK;
                    liveContext.getMessageService().leaveLinkMic(reason, null);
                    // ??????????????????
                    updateVisible();
                    break;
                case Actions.LINK_MIC_MANAGE_CLICK:
                    linkMicManageController.show();
                    break;
                case Actions.ANCHOR_PUSH_SUCCESS:
                    if (supportLinkMic()) {
                        afterPushSuccess();
                    }
                    break;
            }
        }

        @Override
        public void onActivityFinish() {
            super.onActivityFinish();
            if (supportLinkMic() && isJoinedLinkMic && pushManager != null) {
                pushManager.stopPublish();
                pushManager.stopPull();
                pushManager.release();
                liveContext.getMessageService().leaveLinkMic(LeaveLinkMicModel.REASON_BY_SELF, null);
            }
        }

        private void updateMixStreamLayout() {
            List<LiveLinkMicPushManager.MixItem> mixItems = new ArrayList<>();
            List<LinkMicItemModel> dataList = recyclerViewHelper.getDataList();
            for (int i = 0; i < dataList.size(); i++) {
                LinkMicItemModel model = dataList.get(i);
                LiveLinkMicPushManager.MixItem mixItem = new LiveLinkMicPushManager.MixItem();
                mixItem.userId = model.userId;
                mixItem.isAnchor = TextUtils.equals(model.userId, anchorId);
                RecyclerView.ViewHolder holder = recyclerViewHelper.getRecyclerView().findViewHolderForAdapterPosition(i);
                if (holder != null) {
                    mixItem.renderContainer = holder.itemView.findViewById(R.id.item_container);
                }
                mixItems.add(mixItem);
            }
            pushManager.updateMixItems(mixItems);
        }

        public View getAnchorPreviewView() {
            return liveContext.getAnchorPreviewHolder().getPreviewView();
        }
    }
}
