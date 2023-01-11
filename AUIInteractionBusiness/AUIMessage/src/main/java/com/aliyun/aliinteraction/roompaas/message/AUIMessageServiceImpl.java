package com.aliyun.aliinteraction.roompaas.message;

import com.alibaba.dingpaas.interaction.ImSendMessageToGroupReq;
import com.alibaba.dingpaas.interaction.ImSendMessageToGroupRsp;
import com.alibaba.dingpaas.interaction.ImSendMessageToGroupUsersReq;
import com.alibaba.dingpaas.interaction.ImSendMessageToGroupUsersRsp;
import com.alibaba.fastjson.JSON;
import com.aliyun.aliinteraction.InteractionEngine;
import com.aliyun.aliinteraction.InteractionService;
import com.aliyun.aliinteraction.base.Callback;
import com.aliyun.aliinteraction.base.Error;
import com.aliyun.aliinteraction.error.Errors;
import com.aliyun.aliinteraction.func.Consumer;
import com.aliyun.aliinteraction.listener.OnMessageListener;
import com.aliyun.aliinteraction.logger.Logger;
import com.aliyun.aliinteraction.model.CancelMuteGroupModel;
import com.aliyun.aliinteraction.model.CancelMuteUserModel;
import com.aliyun.aliinteraction.model.JoinGroupModel;
import com.aliyun.aliinteraction.model.LeaveGroupModel;
import com.aliyun.aliinteraction.model.LikeModel;
import com.aliyun.aliinteraction.model.Message;
import com.aliyun.aliinteraction.model.MuteGroupModel;
import com.aliyun.aliinteraction.model.MuteUserModel;
import com.aliyun.aliinteraction.observable.Observable;
import com.aliyun.aliinteraction.roompaas.message.listener.AUIMessageListener;
import com.aliyun.aliinteraction.roompaas.message.model.ApplyJoinLinkMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.CameraStatusUpdateModel;
import com.aliyun.aliinteraction.roompaas.message.model.CancelApplyJoinLinkMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.CommandUpdateCameraModel;
import com.aliyun.aliinteraction.roompaas.message.model.CommandUpdateMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.CommentModel;
import com.aliyun.aliinteraction.roompaas.message.model.HandleApplyJoinLinkMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.JoinLinkMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.KickUserFromLinkMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.LeaveLinkMicModel;
import com.aliyun.aliinteraction.roompaas.message.model.MicStatusUpdateModel;
import com.aliyun.aliinteraction.roompaas.message.model.StartLiveModel;
import com.aliyun.aliinteraction.roompaas.message.model.StopLiveModel;
import com.aliyun.aliinteraction.roompaas.message.model.UpdateNoticeModel;
import com.aliyun.aliinteraction.util.Util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * @author puke
 * @version 2022/8/31
 */
class AUIMessageServiceImpl extends Observable<AUIMessageListener> implements AUIMessageService {

    private static final String TAG = "AUIMessageServiceImpl";

    private final String groupId;
    private final InteractionService service;

    AUIMessageServiceImpl(String groupId) {
        this.groupId = groupId;
        InteractionEngine engine = InteractionEngine.instance();
        this.service = engine.getInteractionService();
        engine.setMessageListener(groupId, new MessageListenerDispatcher());
    }

    @Override
    public void sendComment(String content, Callback<String> callback) {
        CommentModel message = new CommentModel();
        message.content = content;
        doSendMessage(message, null, callback);
    }

    @Override
    public void startLive(Callback<String> callback) {
        doSendMessage(new StartLiveModel(), null, callback);
    }

    @Override
    public void stopLive(Callback<String> callback) {
        doSendMessage(new StopLiveModel(), null, callback);
    }

    @Override
    public void updateNotice(String notice, Callback<String> callback) {
        UpdateNoticeModel model = new UpdateNoticeModel();
        model.notice = notice;
        doSendMessage(model, null, callback);
    }

    @Override
    public void applyJoinLinkMic(String receiveId, Callback<String> callback) {
        ApplyJoinLinkMicModel model = new ApplyJoinLinkMicModel();
        doSendMessage(model, receiveId, callback);
    }

    @Override
    public void handleApplyJoinLinkMic(boolean agree, String applyUserId, String rtcPullUrl, Callback<String> callback) {
        HandleApplyJoinLinkMicModel model = new HandleApplyJoinLinkMicModel();
        model.agree = agree;
        if (agree) {
            model.rtcPullUrl = rtcPullUrl;
        }
        doSendMessage(model, applyUserId, callback);
    }

    @Override
    public void joinLinkMic(String rtcPullUrl, Callback<String> callback) {
        JoinLinkMicModel model = new JoinLinkMicModel();
        model.rtcPullUrl = rtcPullUrl;
        doSendMessage(model, null, callback);
    }

    @Override
    public void leaveLinkMic(String reason, Callback<String> callback) {
        LeaveLinkMicModel model = new LeaveLinkMicModel();
        doSendMessage(model, null, callback);
    }

    @Override
    public void kickUserFromLinkMic(String userId, Callback<String> callback) {
        KickUserFromLinkMicModel model = new KickUserFromLinkMicModel();
        doSendMessage(model, userId, callback);
    }

    @Override
    public void updateMicStatus(boolean opened, Callback<String> callback) {
        MicStatusUpdateModel model = new MicStatusUpdateModel();
        model.micOpened = opened;
        doSendMessage(model, null, callback);
    }

    @Override
    public void updateCameraStatus(boolean opened, Callback<String> callback) {
        CameraStatusUpdateModel model = new CameraStatusUpdateModel();
        model.cameraOpened = opened;
        doSendMessage(model, null, callback);
    }

    @Override
    public void cancelApplyJoinLinkMic(String receiveId, Callback<String> callback) {
        CancelApplyJoinLinkMicModel model = new CancelApplyJoinLinkMicModel();
        doSendMessage(model, receiveId, callback);
    }

    @Override
    public void commandUpdateMic(String receiveId, boolean open, Callback<String> callback) {
        CommandUpdateMicModel model = new CommandUpdateMicModel();
        model.needOpenMic = open;
        doSendMessage(model, receiveId, callback);
    }

    @Override
    public void commandUpdateCamera(String receiveId, boolean open, Callback<String> callback) {
        CommandUpdateCameraModel model = new CommandUpdateCameraModel();
        model.needOpenCamera = open;
        doSendMessage(model, receiveId, callback);
    }

    @Override
    public void addMessageListener(AUIMessageListener messageListener) {
        register(messageListener);
    }

    @Override
    public void removeMessageListener(AUIMessageListener messageListener) {
        unregister(messageListener);
    }

    @Override
    public void removeAllMessageListeners() {
        unregisterAll();
    }

    private void doSendMessage(Serializable messageModel, String directUserId, Callback<String> callback) {
        Class<?> messageType = messageModel.getClass();
        Integer type = AUIMessageTypeMapping.getTypeFromModelClass(messageType);
        if (type == null) {
            Logger.e(TAG, String.format("doSendMessage, class '%s' not declare in %s's method",
                    messageType.getName(), AUIMessageListener.class.getName()));
            Util.callError(callback, Errors.BIZ_ERROR);
            return;
        }

        String data = JSON.toJSONString(messageModel);
        if (directUserId == null) {
            doSendMessageToGroup(data, callback, type);
        } else {
            ArrayList<String> receiverIdList = new ArrayList<>();
            receiverIdList.add(directUserId);
            doSendMessageToGroupUsers(data, receiverIdList, callback, type);
        }
    }

    private void doSendMessageToGroup(String data, final Callback<String> callback, Integer type) {
        ImSendMessageToGroupReq req = new ImSendMessageToGroupReq();
        req.groupId = groupId;
        req.type = type;
        req.data = data;
        req.skipMuteCheck = isSkipMuteCheck(type);
        req.skipAudit = isSkipAudit(type);
        service.sendMessageToGroup(req, new Callback<ImSendMessageToGroupRsp>() {
            @Override
            public void onSuccess(ImSendMessageToGroupRsp rsp) {
                Util.callSuccess(callback, rsp.messageId);
            }

            @Override
            public void onError(Error error) {
                Util.callError(callback, error);
            }
        });
    }

    private void doSendMessageToGroupUsers(String data, ArrayList<String> receiverIdList, final Callback<String> callback, Integer type) {
        ImSendMessageToGroupUsersReq req = new ImSendMessageToGroupUsersReq();
        req.groupId = groupId;
        req.type = type;
        req.data = data;
        req.skipMuteCheck = isSkipMuteCheck(type);
        req.skipAudit = isSkipAudit(type);
        req.receiverIdList = receiverIdList;
        service.sendMessageToGroupUsers(req, new Callback<ImSendMessageToGroupUsersRsp>() {
            @Override
            public void onSuccess(ImSendMessageToGroupUsersRsp rsp) {
                Util.callSuccess(callback, rsp.messageId);
            }

            @Override
            public void onError(Error error) {
                Util.callError(callback, error);
            }
        });
    }

    private boolean isSkipMuteCheck(Integer type) {
        // 非弹幕消息, 均跳过禁言检测
        return type != CommentModel.MESSAGE_TYPE_COMMENT;
    }

    private boolean isSkipAudit(Integer type) {
        // 非弹幕消息, 均跳过安全审核
        return type != CommentModel.MESSAGE_TYPE_COMMENT;
    }

    private class MessageListenerDispatcher implements OnMessageListener {

        @Override
        public void onLikeReceived(final Message<LikeModel> message) {
            Logger.i(TAG, "onLikeReceived");
            dispatch(new Consumer<AUIMessageListener>() {
                @Override
                public void accept(AUIMessageListener listener) {
                    listener.onLikeReceived(message);
                }
            });
        }

        @Override
        public void onJoinGroup(final Message<JoinGroupModel> message) {
            Logger.i(TAG, "onJoinGroup");
            dispatch(new Consumer<AUIMessageListener>() {
                @Override
                public void accept(AUIMessageListener listener) {
                    listener.onJoinGroup(message);
                }
            });
        }

        @Override
        public void onLeaveGroup(final Message<LeaveGroupModel> message) {
            Logger.i(TAG, "onLeaveGroup");
            dispatch(new Consumer<AUIMessageListener>() {
                @Override
                public void accept(AUIMessageListener listener) {
                    listener.onLeaveGroup(message);
                }
            });
        }

        @Override
        public void onMuteGroup(final Message<MuteGroupModel> message) {
            Logger.i(TAG, "onMuteGroup");
            dispatch(new Consumer<AUIMessageListener>() {
                @Override
                public void accept(AUIMessageListener listener) {
                    listener.onMuteGroup(message);
                }
            });
        }

        @Override
        public void onCancelMuteGroup(final Message<CancelMuteGroupModel> message) {
            Logger.i(TAG, "onCancelMuteGroup");
            dispatch(new Consumer<AUIMessageListener>() {
                @Override
                public void accept(AUIMessageListener listener) {
                    listener.onCancelMuteGroup(message);
                }
            });
        }

        @Override
        public void onMuteUser(final Message<MuteUserModel> message) {
            Logger.i(TAG, "onMuteUser");
            dispatch(new Consumer<AUIMessageListener>() {
                @Override
                public void accept(AUIMessageListener listener) {
                    listener.onMuteUser(message);
                }
            });
        }

        @Override
        public void onCancelMuteUser(final Message<CancelMuteUserModel> message) {
            Logger.i(TAG, "onCancelMuteUser");
            dispatch(new Consumer<AUIMessageListener>() {
                @Override
                public void accept(AUIMessageListener listener) {
                    listener.onCancelMuteUser(message);
                }
            });
        }

        @Override
        public void onCustomMessageReceived(final Message<String> message) {
            Logger.i(TAG, "onCustomMessageReceived, message.type=" + message.type);
            dispatch(new Consumer<AUIMessageListener>() {
                @Override
                public void accept(AUIMessageListener auiMessageListener) {
                    Message<String> convertedMessage = MessageListenerDispatcher.this.copyMessageWithoutData(message);
                    convertedMessage.data = message.data;
                    auiMessageListener.onRawMessageReceived(convertedMessage);
                }
            });

            int type = message.type;
            AUIMessageTypeMapping.CallbackInfo callbackInfo = AUIMessageTypeMapping.getCallbackInfoFromType(type);
            if (callbackInfo == null) {
                Logger.w(TAG, "onMessageReceived, unknown type: " + type);
                return;
            }

            Class<?> modelClass = callbackInfo.modelClass;
            final Object data;
            try {
                data = JSON.parseObject(message.data, modelClass);
            } catch (Exception e) {
                e.printStackTrace();
                Logger.e(TAG, "onMessageReceived, parse json error for " + modelClass);
                // TODO: 2022/9/20 此处return需多端保持对齐, 防止其他端data传null或空字符串
                return;
            }

            @SuppressWarnings("rawtypes") final Message convertedMessage = copyMessageWithoutData(message);
            convertedMessage.data = data;

            final Method auiMessageListenerMethod = callbackInfo.callbackMethod;
            if (auiMessageListenerMethod == null) {
                Logger.e(TAG, "onMessageReceived, can't find method of callback for " + modelClass);
                return;
            }

            Logger.i(TAG, "onCustomMessageReceived, invoke: " + auiMessageListenerMethod.getName());
            dispatch(new Consumer<AUIMessageListener>() {
                @Override
                public void accept(AUIMessageListener auiMessageListener) {
                    try {
                        auiMessageListenerMethod.invoke(auiMessageListener, convertedMessage);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                        Logger.e(TAG, "onMessageReceived, invoke error: " + e.getMessage(), e);
                    }
                }
            });
        }

        private <T> Message<T> copyMessageWithoutData(Message<?> message) {
            Message<T> auiMessage = new Message<>();
            auiMessage.messageId = message.messageId;
            auiMessage.groupId = message.groupId;
            auiMessage.senderId = message.senderId;
            auiMessage.senderInfo = message.senderInfo;
            auiMessage.type = message.type;
            return auiMessage;
        }
    }
}
