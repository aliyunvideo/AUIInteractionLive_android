package com.aliyun.aliinteraction.liveroom.util;

import com.aliyun.aliinteraction.uikit.uibase.helper.SpHelper;

import java.io.Serializable;

/**
 * @author puke
 * @version 2021/5/24
 */
@SpHelper.Sp
public interface UserSp extends Serializable {

    @SpHelper.Setter
    void setUserId(String userId);

    @SpHelper.Getter
    String getUserId();

    @SpHelper.Setter
    void setNick(String nick);

    @SpHelper.Getter
    String getNick();
}
