package com.aliyun.aliinteraction.liveroom;

import android.content.Context;
import android.content.Intent;

/**
 * Created by KyleCe on 2021/7/6
 */
class Router {

    static void openBusinessRoomPage(Context context, LiveParam liveParam) {
        Intent intent = new Intent(context, LiveActivity.class);
        intent.putExtra(LiveConst.PARAM_KEY_LIVE_PARAM, liveParam);
        context.startActivity(intent);
    }
}
