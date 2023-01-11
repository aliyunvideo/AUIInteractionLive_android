package com.aliyun.aliinteraction.liveroom;

import com.aliyun.aliinteraction.liveroom.model.LiveModel;
import com.aliyun.aliinteraction.uikit.uibase.activity.BaseActivity;

/**
 * 房间Activity, 封装房间相关的通用处理逻辑
 *
 * @author puke
 * @version 2021/5/27
 */
public abstract class BaseRoomActivity extends BaseActivity {

    protected abstract String getLiveId();

    protected abstract LiveModel getLiveModel();

    /**
     * 进房间成功的回调
     *
     * @param liveModel 房间信息
     */
    protected abstract void onEnterRoomSuccess(LiveModel liveModel);

    /**
     * 进房间失败的回调
     *
     * @param errorMsg 错误信息
     */
    protected abstract void onEnterRoomError(String errorMsg);

    @Override
    public void finish() {
        super.finish();
    }
}
