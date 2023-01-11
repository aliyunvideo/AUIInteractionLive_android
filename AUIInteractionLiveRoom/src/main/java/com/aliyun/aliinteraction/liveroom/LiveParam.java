package com.aliyun.aliinteraction.liveroom;

import com.aliyun.aliinteraction.liveroom.model.LiveModel;

import java.io.Serializable;

/**
 * @author puke
 * @version 2021/12/15
 */
public class LiveParam implements Serializable {

    public String liveId;
    public LiveModel liveModel;
    public LivePrototype.Role role;
    public String userNick;
    public String userExtension;
    public String notice;
}
