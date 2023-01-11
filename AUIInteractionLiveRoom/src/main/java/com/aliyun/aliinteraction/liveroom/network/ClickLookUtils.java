package com.aliyun.aliinteraction.liveroom.network;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按照设定间隔允许再次点击，避免点击过快触发多次。
 * <p>
 * 针对同一触发者View生效
 */
public class ClickLookUtils {
        // 两次点击按钮之间的点击间隔不能少于1000毫秒
        private static final int MIN_CLICK_DELAY_TIME = 1000;
        private static long lastClickTime;

        public static boolean isFastClick() {
            boolean flag = false;
            long curClickTime = System.currentTimeMillis();
            if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
                flag = true;
            }
            lastClickTime = curClickTime;
            return flag;
        }
}