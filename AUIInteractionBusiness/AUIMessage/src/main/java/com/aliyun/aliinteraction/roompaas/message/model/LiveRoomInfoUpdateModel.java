package com.aliyun.aliinteraction.roompaas.message.model;

import com.aliyun.aliinteraction.roompaas.message.annotation.MessageType;

import java.io.Serializable;

/**
 * @author puke
 * @version 2022/9/20
 */
@MessageType(10005)
public class LiveRoomInfoUpdateModel implements Serializable {

    /**
     * 直播间pv
     */
    public long pv;
}
