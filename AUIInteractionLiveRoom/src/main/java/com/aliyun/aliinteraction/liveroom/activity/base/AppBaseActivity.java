package com.aliyun.aliinteraction.liveroom.activity.base;

import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.Nullable;

import com.aliyun.aliinteraction.uikit.uibase.activity.BaseActivity;
import com.aliyun.aliinteraction.core.assist.Assist;
import com.aliyun.aliinteraction.liveroom.util.AppUtil;

/**
 * 通用Activity, 封装最基础的复用逻辑
 *
 * @author puke
 * @version 2021/5/13
 */
public class AppBaseActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtil.initMoTuAliHaIfNecessary(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 点击音量减按键, 进入配置页面
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Assist.openConfigPage(this);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
