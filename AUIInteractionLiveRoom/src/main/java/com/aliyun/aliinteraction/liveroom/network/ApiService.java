package com.aliyun.aliinteraction.liveroom.network;

import com.aliyun.aliinteraction.liveroom.model.AppServerToken;
import com.aliyun.aliinteraction.liveroom.model.CreateLiveRequest;
import com.aliyun.aliinteraction.liveroom.model.GetLiveRequest;
import com.aliyun.aliinteraction.liveroom.model.GetMeetingInfoRequest;
import com.aliyun.aliinteraction.liveroom.model.ListLiveRequest;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;
import com.aliyun.aliinteraction.liveroom.model.LoginRequest;
import com.aliyun.aliinteraction.liveroom.model.MeetingInfo;
import com.aliyun.aliinteraction.liveroom.model.StartLiveRequest;
import com.aliyun.aliinteraction.liveroom.model.StopLiveRequest;
import com.aliyun.aliinteraction.liveroom.model.Token;
import com.aliyun.aliinteraction.liveroom.model.TokenRequest;
import com.aliyun.aliinteraction.liveroom.model.UpdateLiveRequest;
import com.aliyun.aliinteraction.liveroom.model.UpdateMeetingInfoRequest;

import java.util.List;

import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * @author puke
 * @version 2022/8/25
 */
public interface ApiService {

    @POST("live/login")
    //登录
    ApiInvoker<AppServerToken> login(@Body LoginRequest request);
    //获取token校验合法性
    @POST("live/token")
    ApiInvoker<Token> getToken(@Body TokenRequest request);
    //创建直播间
    @POST("live/create")
    ApiInvoker<LiveModel> createLive(@Body CreateLiveRequest request);
     //更新直播间信息，比如更新公告等
    @POST("live/update")
    ApiInvoker<LiveModel> updateLive(@Body UpdateLiveRequest request);
    //获取直播间信息，方便进入直播间信息展示
    @POST("live/get")
    ApiInvoker<LiveModel> getLive(@Body GetLiveRequest request);
    //推流成功后, 调用此服务通知服务端更新状态（开播状态）
    @POST("live/start")
    ApiInvoker<Void> startLive(@Body StartLiveRequest request);
    //停止推流后, 调用此服务通知服务端更新状态（停播状态）
    @POST("live/stop")
    ApiInvoker<Void> stopLive(@Body StopLiveRequest request);
    //获取直播间列表
    @POST("live/list")
    ApiInvoker<List<LiveModel>> getLiveList(@Body ListLiveRequest request);
    //主播将最新的麦上成员列表更新到AppServer端，直播间连麦管理模块用
    @POST("live/updateMeetingInfo")
    ApiInvoker<MeetingInfo> updateMeetingInfo(@Body UpdateMeetingInfoRequest request);
    //获取连麦观众信息，直播间连麦管理模块用
    @POST("live/getMeetingInfo")
    ApiInvoker<MeetingInfo> getMeetingInfo(@Body GetMeetingInfoRequest request);
}
