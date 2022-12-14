package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.dingpaas.interaction.ImCancelMuteUserReq;
import com.alibaba.dingpaas.interaction.ImCancelMuteUserRsp;
import com.alibaba.dingpaas.interaction.ImMuteUserReq;
import com.alibaba.dingpaas.interaction.ImMuteUserRsp;
import com.alibaba.fastjson.JSON;
import com.aliyun.aliinteraction.base.ToastCallback;
import com.aliyun.aliinteraction.common.base.log.Logger;
import com.aliyun.aliinteraction.common.base.util.CommonUtil;
import com.aliyun.aliinteraction.core.base.Actions;
import com.aliyun.aliinteraction.core.base.LimitSizeRecyclerView;
import com.aliyun.aliinteraction.core.base.MessageModel;
import com.aliyun.aliinteraction.core.utils.MessageHelper;
import com.aliyun.aliinteraction.enums.BroadcastType;
import com.aliyun.aliinteraction.liveroom.AppConfig;
import com.aliyun.aliinteraction.liveroom.BaseComponent;
import com.aliyun.aliinteraction.liveroom.ComponentHolder;
import com.aliyun.aliinteraction.liveroom.IComponent;
import com.aliyun.aliinteraction.liveroom.LiveConst;
import com.aliyun.aliinteraction.liveroom.LiveContext;
import com.aliyun.aliinteraction.liveroom.R;
import com.aliyun.aliinteraction.model.CancelMuteGroupModel;
import com.aliyun.aliinteraction.model.CancelMuteUserModel;
import com.aliyun.aliinteraction.model.JoinGroupModel;
import com.aliyun.aliinteraction.model.LeaveGroupModel;
import com.aliyun.aliinteraction.model.Message;
import com.aliyun.aliinteraction.model.MuteGroupModel;
import com.aliyun.aliinteraction.model.MuteUserModel;
import com.aliyun.aliinteraction.roompaas.message.listener.SimpleOnMessageListener;
import com.aliyun.aliinteraction.roompaas.message.model.CommentModel;
import com.aliyun.aliinteraction.roompaas.message.model.StartLiveModel;
import com.aliyun.aliinteraction.roompaas.message.model.StopLiveModel;
import com.aliyun.aliinteraction.uikit.uibase.helper.RecyclerViewHelper;
import com.aliyun.aliinteraction.uikit.uibase.util.AppUtil;
import com.aliyun.aliinteraction.uikit.uibase.util.DialogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author puke
 * @version 2021/7/29
 */
public class LiveMessageView extends RelativeLayout implements ComponentHolder {

    private static final String TAG = LiveMessageView.class.getSimpleName();
    private static final int NICK_SHOW_MAX_LENGTH = 15;
    private static final String WELCOME_TEXT = "???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????";

    protected final FlyView flyView;
    protected final LimitSizeRecyclerView recyclerView;

    private final MessageHelper<MessageModel> messageHelper;
    private final Component component = new Component();
    private final LinearLayoutManager layoutManager;
    private final RecyclerViewHelper<MessageModel> recyclerViewHelper;
    private final int commentMaxHeight = AppUtil.getScreenHeight() / 3;
    private final Runnable refreshUITask = new Runnable() {
        @Override
        public void run() {
            recyclerView.invalidate();
        }
    };

    private int lastPosition;
    private boolean forceHover;

    public LiveMessageView(Context context) {
        this(context, null, 0);
    }

    public LiveMessageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveMessageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View.inflate(context, R.layout.ilr_view_live_message, this);
        flyView = findViewById(R.id.message_fly_view);
        recyclerView = findViewById(R.id.message_recycler_view);

        // ????????????
        recyclerView.setMaxHeight(commentMaxHeight);
        layoutManager = new LinearLayoutManager(context);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerViewHelper = RecyclerViewHelper.of(recyclerView, R.layout.ilr_item_message,
                new RecyclerViewHelper.HolderRenderer<MessageModel>() {
                    @Override
                    public void render(RecyclerViewHelper<MessageModel> viewHelper, RecyclerViewHelper.ViewHolder holder, final MessageModel model, int position, int itemCount, List<Object> payloads) {
                        TextView content = holder.getView(R.id.item_content);
                        content.setMovementMethod(new LinkMovementMethod());
                        content.setTextColor(model.contentColor);

                        if (TextUtils.isEmpty(model.userNick)) {
                            content.setText(model.content);
                        } else {
                            String prefix = model.userNick + "???";
                            String postfix = model.content;

                            SpannableString spannableString = new SpannableString(prefix + postfix);
                            spannableString.setSpan(new ForegroundColorSpan(model.color), 0,
                                    prefix.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                            if (component.isOwner()) {
                                ClickableSpan clickableSpan = new ClickableSpan() {
                                    @Override
                                    public void onClick(@NonNull View widget) {
                                        component.handleUserClick(model);
                                    }

                                    @Override
                                    public void updateDrawState(@NonNull TextPaint ds) {
                                        ds.setUnderlineText(false);
                                    }
                                };
                                spannableString.setSpan(clickableSpan,
                                        0, prefix.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                            }
                            content.setText(spannableString);
                        }

                        content.setOnLongClickListener(new OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                if (!TextUtils.isEmpty(model.userId)) {
                                    // do something
                                }
                                return true;
                            }
                        });

                        content.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (!TextUtils.isEmpty(model.userId)) {
                                    // do something
                                }
                            }
                        });
                    }
                });

        // ????????????????????????
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                refreshUnreadTips();
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                forceHover = false;
                refreshUnreadTips();
            }
        });

        // ?????????????????????
        messageHelper = new MessageHelper<MessageModel>()
                .setCallback(new MessageHelper.Callback<MessageModel>() {
                    @Override
                    public int getTotalSize() {
                        return recyclerViewHelper.getItemCount();
                    }

                    @Override
                    public void onMessageAdded(MessageModel message) {
                        addMessageToPanel(Collections.singletonList(message));
                    }

                    @Override
                    public void onMessageRemoved(int suggestRemoveCount) {
                        lastPosition -= suggestRemoveCount;
                        if (forceHover) {
                            postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    forceHover = true;
                                }
                            }, 10);
                        }
                        recyclerViewHelper.removeDataWithoutAnimation(0, suggestRemoveCount);
                    }
                });



    }

    /**
     * @return ?????????????????????????????????
     */
    protected boolean enableUnreadTipsLogic() {
        return true;
    }

    /**
     * @return ????????????????????????????????????
     */
    protected boolean enableSystemLogic() {
        return true;
    }

    private void refreshUnreadTips() {
        if (!enableUnreadTipsLogic()) {
            // ?????????????????????????????????, ??????????????????
            return;
        }

        int itemCount = recyclerViewHelper.getItemCount();
        int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        if (lastPosition >= itemCount) {
            lastPosition = lastVisibleItemPosition;
        } else {
            lastPosition = Math.max(lastVisibleItemPosition, lastPosition);
        }

        if (forceHover || (lastPosition >= 0 && lastPosition < itemCount - 1)) {
            // ????????????, ???????????????????????????, ????????????????????????
            forceHover = true;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        final int maxMessageHeight;
        if (component.isLandscape()) {
            // ??????
            maxMessageHeight = AppUtil.getScreenHeight() / 3;
            // ?????????????????????
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                    AppUtil.getScreenWidth() / 2, MeasureSpec.getMode(widthMeasureSpec)
            );
        } else {
            // ??????
            int systemMaxHeight = enableSystemLogic() ? (getResources().getDimensionPixelOffset(R.dimen.live_message_fly_height) + getResources().getDimensionPixelOffset(R.dimen.message_fly_bottom_margin)) : 0;
            maxMessageHeight = commentMaxHeight + systemMaxHeight;
        }
        if (height > maxMessageHeight) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    maxMessageHeight, MeasureSpec.getMode(heightMeasureSpec)
            );
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void addSystemMessage(CharSequence content) {
        FlyView.FlyItem item = new FlyView.FlyItem();
        item.content = content;
        addSystemMessage(item);
    }

    protected void addSystemMessage(FlyView.FlyItem item) {
        if (enableSystemLogic()) {
            // ??????????????????????????????, ????????????
            flyView.addItem(item);
        }
    }

    protected void addMessage(String type, String content) {
        addMessage(new MessageModel(type, content));
    }

    protected void addMessage(MessageModel messageModel) {
        messageHelper.addMessage(messageModel);
    }

    protected void addMessageByUserId(String userId, String type, String content) {
        messageHelper.addMessage(new MessageModel(userId, type, content));
    }

    /**
     * ???????????????????????????
     *
     * @param addedList ????????????
     */
    protected void addMessageToPanel(final List<MessageModel> addedList) {
        boolean isLastCompletelyVisible = layoutManager.findLastVisibleItemPosition()
                == recyclerViewHelper.getItemCount() - 1;
        recyclerViewHelper.addData(addedList);
        if (!forceHover && isLastCompletelyVisible) {
            // ????????????, ???????????????
            layoutManager.scrollToPositionWithOffset(
                    recyclerViewHelper.getItemCount() - 1, Integer.MIN_VALUE);
            postDelayed(refreshUITask, 100);
            lastPosition = 0;
        } else {
            refreshUnreadTips();
        }
    }

    /**
     * @return ?????????????????? (??????null???, ?????????)
     */
    @Nullable
    protected MessageModel getSystemAlertMessageModel() {
        MessageModel systemMessage = new MessageModel(
                LiveConst.SYSTEM_NOTICE_NICKNAME, LiveConst.SYSTEM_NOTICE_ALERT);
        systemMessage.contentColor = Color.parseColor("#12DBE6");
        return systemMessage;
    }

    @Override
    public IComponent getComponent() {
        return component;
    }

    protected static String truncateNick(String nick) {
        if (!TextUtils.isEmpty(nick) && nick.length() > NICK_SHOW_MAX_LENGTH) {
            nick = nick.substring(0, NICK_SHOW_MAX_LENGTH);
        }
        return nick;
    }

    private class Component extends BaseComponent {
        @Override
        public void onInit(LiveContext liveContext) {
            super.onInit(liveContext);

            // ???????????????????????????
            setVisibility(VISIBLE);

            if(!needPlayback()) {
                MessageModel model = new MessageModel(null, WELCOME_TEXT);
                model.contentColor = Color.parseColor("#A4E0A7");
                addMessageToPanel(Collections.singletonList(model));
            }

            getMessageService().addMessageListener(new SimpleOnMessageListener() {
                @Override
                public void onCommentReceived(Message<CommentModel> message) {
                    String senderId = message.senderId;
                    if (AppConfig.INSTANCE.showSelfCommentFromLocal()
                            && TextUtils.equals(senderId, getUserId())) {
                        // ???????????????????????????????????????
                        return;
                    }
                    String nick = truncateNick(message.senderInfo.userNick);
                    addMessageByUserId(senderId, nick, message.data.content);
                }

                @Override
                public void onStopLive(Message<StopLiveModel> message) {
                    if (!isOwner()) {
                        addSystemMessage("???????????????");
                    }
                }

                @Override
                public void onStartLive(Message<StartLiveModel> message) {
                    if (!isOwner()) {
                        addSystemMessage("???????????????");
                    }
                }

                @Override
                public void onJoinGroup(Message<JoinGroupModel> message) {
                    String userNick = message.senderInfo.userNick;
                    if (!TextUtils.isEmpty(userNick)) {
                        // addSystemMessage(truncateNick(userNick) + "??????????????????");
                    }
                }

                @Override
                public void onLeaveGroup(Message<LeaveGroupModel> message) {
                    String userNick = message.senderInfo.userNick;
                    if (!TextUtils.isEmpty(userNick)) {
                        // addSystemMessage(truncateNick(userNick) + "??????????????????");
                    }
                }

                @Override
                public void onMuteGroup(Message<MuteGroupModel> message) {
                    addSystemMessage("???????????????????????????");
                }

                @Override
                public void onCancelMuteGroup(Message<CancelMuteGroupModel> message) {
                    addSystemMessage("???????????????????????????");
                }

                @Override
                public void onMuteUser(Message<MuteUserModel> message) {
                    boolean isSelf = TextUtils.equals(getUserId(), message.data.userId);
                    String subject = isSelf ? "???" : truncateNick(message.data.userNick);
                    addSystemMessage(String.format("%s??????????????????", subject));
                }

                @Override
                public void onCancelMuteUser(Message<CancelMuteUserModel> message) {
                    boolean isSelf = TextUtils.equals(getUserId(), message.data.userId);
                    String subject = isSelf ? "???" : truncateNick(message.data.userNick);
                    addSystemMessage(String.format("%s????????????????????????", subject));
                }

                @Override
                public void onRawMessageReceived(Message<String> message) {
                    // ??????????????????, ???????????????????????????
                    if (AppConfig.INSTANCE.enableAllMessageReceived()) {
                        addMessageToPanel(Collections.singletonList(new MessageModel(
                                message.senderId,
                                String.format("Raw(%s)", message.type),
                                message.data
                        )));
                    }
                }
            });
        }

        @Override
        public void onEvent(String action, Object... args) {
            if (Actions.SHOW_MESSAGE.equals(action)) {
                if (!AppConfig.INSTANCE.showSelfCommentFromLocal()) {
                    // ???????????????, ?????????
                    return;
                }

                if (args.length >= 1) {
                    MessageModel messageModel = (MessageModel) args[0];
                    // ????????????????????????????????????
                    boolean ignoreFreqLimit = args.length > 1 && Boolean.TRUE.equals(args[1]);
                    if (ignoreFreqLimit) {
                        // ?????????????????????, ????????????
                        addMessageToPanel(Collections.singletonList(messageModel));
                    } else {
                        // ???????????????????????????
                        addMessage(messageModel);
                    }
                } else {
                    Logger.w(TAG, "Received invalid message param: " + JSON.toJSONString(args));
                }
            }
        }

        @Override
        public void onActivityDestroy() {
            if (messageHelper != null) {
                messageHelper.destroy();
            }
        }

        private void handleUserClick(final MessageModel model) {
            String userId = model.userId;
            if (TextUtils.isEmpty(userId)) {
                CommonUtil.showToast(getContext(), "??????ID??????");
                return;
            }

            DialogUtil.doAction(
                    getContext(), String.format("???%s??????", model.userNick),
                    new DialogUtil.Action("??????", new Runnable() {
                        @Override
                        public void run() {
                            ImMuteUserReq req = new ImMuteUserReq();
                            req.groupId = component.getGroupId();
                            req.broadCastType = BroadcastType.SPECIFIC.getValue();
                            req.muteUserList = new ArrayList<String>() {{
                                add(model.userId);
                            }};
                            interactionService.muteUser(req, new ToastCallback<ImMuteUserRsp>("??????"));
                        }
                    }),
                    new DialogUtil.Action("????????????", new Runnable() {
                        @Override
                        public void run() {
                            ImCancelMuteUserReq req = new ImCancelMuteUserReq();
                            req.groupId = component.getGroupId();
                            req.broadCastType = BroadcastType.SPECIFIC.getValue();
                            req.cancelMuteUserList = new ArrayList<String>() {{
                                add(model.userId);
                            }};
                            interactionService.cancelMuteUser(req, new ToastCallback<ImCancelMuteUserRsp>("????????????"));
                        }
                    })
            );
        }
    }
}
