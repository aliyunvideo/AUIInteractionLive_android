package com.aliyun.aliinteraction.roompaas.message.listener;

import com.aliyun.aliinteraction.listener.OnMessageListener;
import com.aliyun.aliinteraction.model.Message;
import com.aliyun.aliinteraction.roompaas.message.annotation.IgnoreMapping;
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
public interface AUIMessageListener extends OnMessageListener {

    /**
     * 收到弹幕消息
     */
    void onCommentReceived(Message<CommentModel> message);

    /**
     * 开始直播
     */
    void onStartLive(Message<StartLiveModel> message);

    /**
     * 结束直播
     */
    void onStopLive(Message<StopLiveModel> message);

    /**
     * 更新公告
     */
    void onNoticeUpdate(Message<UpdateNoticeModel> message);

    /**
     * 申请连麦
     */
    void onApplyJoinLinkMic(Message<ApplyJoinLinkMicModel> message);

    /**
     * 处理连麦申请
     */
    void onHandleApplyJoinLinkMic(Message<HandleApplyJoinLinkMicModel> message);

    /**
     * 上麦通知
     */
    void onJoinLinkMic(Message<JoinLinkMicModel> message);

    /**
     * 下麦通知
     */
    void onLeaveLinkMic(Message<LeaveLinkMicModel> message);

    /**
     * 踢下麦
     */
    void onKickUserFromLinkMic(Message<KickUserFromLinkMicModel> message);

    /**
     * 麦克风状态变化
     */
    void onMicStatusUpdate(Message<MicStatusUpdateModel> message);

    /**
     * 摄像头状态变化
     */
    void onCameraStatusUpdate(Message<CameraStatusUpdateModel> message);

    /**
     * 命令更改麦克风状态消息
     */
    void onCommandMicUpdate(Message<CommandUpdateMicModel> message);

    /**
     * 命令更改摄像头状态消息
     */
    void onCommandCameraUpdate(Message<CommandUpdateCameraModel> message);

    /**
     * 取消申请连麦
     */
    void onCancelApplyJoinLinkMic(Message<CancelApplyJoinLinkMicModel> message);

    /**
     * 原始消息透出 (不做type解析)
     */
    @IgnoreMapping
    void onRawMessageReceived(Message<String> message);
}
