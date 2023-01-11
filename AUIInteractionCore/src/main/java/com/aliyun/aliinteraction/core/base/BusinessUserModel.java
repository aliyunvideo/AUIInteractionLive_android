package com.aliyun.aliinteraction.core.base;

import com.aliyun.aliinteraction.core.utils.ColorUtil;

import java.io.Serializable;

/**
 * @author puke
 * @version 2021/5/21
 */
public class BusinessUserModel implements Serializable {

    public String id;

    public String nick;

    public final int color = ColorUtil.randomColor();

    public BusinessUserModel(String id, String nick) {
        this.id = id;
        this.nick = nick;
    }

    public BusinessUserModel() {
    }
}
