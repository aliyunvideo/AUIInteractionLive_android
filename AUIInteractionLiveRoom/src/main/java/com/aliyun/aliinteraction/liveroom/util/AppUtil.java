package com.aliyun.aliinteraction.liveroom.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aliyun.aliinteraction.common.base.util.Utils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author puke
 * @version 2021/5/12
 */

public class AppUtil extends com.aliyun.aliinteraction.uikit.uibase.util.AppUtil {
    private static final AtomicBoolean isHaInitialized = new AtomicBoolean();

    public static void initMoTuAliHaIfNecessary(Context context) {
        if (isHaInitialized.get() || context == null) {
            return;
        }
        isHaInitialized.set(true);
    }
}
