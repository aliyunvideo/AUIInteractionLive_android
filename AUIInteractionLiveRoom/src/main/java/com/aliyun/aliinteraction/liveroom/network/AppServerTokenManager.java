package com.aliyun.aliinteraction.liveroom.network;

import android.text.TextUtils;

import com.aliyun.aliinteraction.base.Callback;
import com.aliyun.aliinteraction.base.Error;
import com.aliyun.aliinteraction.error.Errors;
import com.aliyun.aliinteraction.liveroom.model.AppServerToken;
import com.aliyun.aliinteraction.liveroom.model.LoginRequest;
import com.aliyun.aliinteraction.util.Util;

/**
 * @author puke
 * @version 2022/10/28
 */
public class AppServerTokenManager {

    private static String username;
    private static String password;
    private static String appServerToken;

    public static String getAppServerToken() {
        return appServerToken;
    }

    public static void login(final String username, final String password, final Callback<Void> callback) {
        LoginRequest request = new LoginRequest();
        request.username = username;
        request.password = password;
        AppServerApi.instance().login(request).invoke(new Callback<AppServerToken>() {
            @Override
            public void onSuccess(AppServerToken data) {
                appServerToken = data.token;
                AppServerTokenManager.username = username;
                AppServerTokenManager.password = password;
                Util.callSuccess(callback);
            }

            @Override
            public void onError(Error error) {
                Util.callError(callback, error);
            }
        });
    }

    public static void refreshToken(final Callback<Void> callback) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Util.callError(callback, Errors.BIZ_ERROR);
        } else {
            login(username, password, callback);
        }
    }
}
