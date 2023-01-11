package com.aliyun.aliinteraction.roompaas.message.listener;

import com.aliyun.aliinteraction.listener.SimpleMessageListener;
import com.aliyun.aliinteraction.model.Message;
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

/**
 * @author puke
 * @version 2022/8/31
 */
public class SimpleOnMessageListener extends SimpleMessageListener implements AUIMessageListener {

    @Override
    public void onCommentReceived(Message<CommentModel> message) {

    }

    @Override
    public void onStartLive(Message<StartLiveModel> message) {

    }

    @Override
    public void onStopLive(Message<StopLiveModel> message) {

    }

    @Override
    public void onNoticeUpdate(Message<UpdateNoticeModel> message) {

    }

    @Override
    public void onApplyJoinLinkMic(Message<ApplyJoinLinkMicModel> message) {

    }

    @Override
    public void onHandleApplyJoinLinkMic(Message<HandleApplyJoinLinkMicModel> message) {

    }

    @Override
    public void onJoinLinkMic(Message<JoinLinkMicModel> message) {

    }

    @Override
    public void onLeaveLinkMic(Message<LeaveLinkMicModel> message) {

    }

    @Override
    public void onKickUserFromLinkMic(Message<KickUserFromLinkMicModel> message) {

    }

    @Override
    public void onMicStatusUpdate(Message<MicStatusUpdateModel> message) {

    }

    @Override
    public void onCameraStatusUpdate(Message<CameraStatusUpdateModel> message) {

    }

    @Override
    public void onCommandMicUpdate(Message<CommandUpdateMicModel> message) {

    }

    @Override
    public void onCommandCameraUpdate(Message<CommandUpdateCameraModel> message) {

    }

    @Override
    public void onCancelApplyJoinLinkMic(Message<CancelApplyJoinLinkMicModel> message) {

    }

    @Override
    public void onRawMessageReceived(Message<String> message) {

    }
}
