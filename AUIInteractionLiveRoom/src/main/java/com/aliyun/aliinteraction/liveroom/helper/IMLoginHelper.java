package com.aliyun.aliinteraction.liveroom.helper;

import android.text.TextUtils;
import android.util.Pair;

import com.alibaba.dingpaas.base.DPSConnectionStatus;
import com.aliyun.aliinteraction.EngineConfig;
import com.aliyun.aliinteraction.IToken;
import com.aliyun.aliinteraction.InteractionEngine;
import com.aliyun.aliinteraction.TokenAccessor;
import com.aliyun.aliinteraction.base.Callback;
import com.aliyun.aliinteraction.base.Error;
import com.aliyun.aliinteraction.listener.SimpleEngineListener;
import com.aliyun.aliinteraction.liveroom.AppConfig;
import com.aliyun.aliinteraction.liveroom.model.Token;
import com.aliyun.aliinteraction.liveroom.model.TokenRequest;
import com.aliyun.aliinteraction.liveroom.network.ApiService;
import com.aliyun.aliinteraction.liveroom.network.AppServerApi;
import com.aliyun.aliinteraction.liveroom.util.TokenParser;
import com.aliyun.aliinteraction.liveroom.util.UserHelper;
import com.aliyun.aliinteraction.util.CommonUtil;
import com.aliyun.aliinteraction.util.Util;

/**
 * IM登录辅助类
 *
 * @author puke
 * @version 2023/1/11
 */
public class IMLoginHelper {

    private static boolean isLoginPending;

    static {
        EngineConfig config = new EngineConfig();
        config.deviceId = CommonUtil.getDeviceId();
        config.tokenAccessor = new TokenAccessor() {
            @Override
            public void getToken(String userId, final Callback<IToken> callback) {
                TokenRequest request = new TokenRequest();
                request.userId = userId;
                request.deviceId = CommonUtil.getDeviceId();
                request.deviceType = "android";
                ApiService apiService = AppServerApi.instance();
                apiService.getToken(request).invoke(new Callback<Token>() {
                    @Override
                    public void onSuccess(Token token) {
                        String longLinkUrl = AppConfig.INSTANCE.longLinkUrl();
                        if (!TextUtils.isEmpty(longLinkUrl)) {
                            Pair<String, String> tokenAndUrl = TokenParser.decodeTokenAndUrl(token.accessToken);
                            token.accessToken = TokenParser.encodeTokenAndUrl(tokenAndUrl.first, longLinkUrl);
                        }
                        callback.onSuccess(token);
                    }

                    @Override
                    public void onError(Error error) {
                        callback.onError(error);
                    }
                });
            }
        };
        InteractionEngine.instance().init(config);
    }

    public static void login(final String userId, final Callback<Void> callback) {
        if (TextUtils.isEmpty(userId)) {
            Util.callError(callback, "用户Id不能为空");
            return;
        }

        if (isLoginPending) {
            Util.callError(callback, "正在进行登录操作, 请稍等");
            return;
        }

        isLoginPending = true;
        InteractionEngine engine = InteractionEngine.instance();
        if (engine.isLogin()) {
            // 1. 已登录, 检查是不是同一个userId
            if (TextUtils.equals(userId, engine.getCurrentUserId())) {
                // 1.1 是同一个, 直接回调成功
                isLoginPending = false;
                Util.callSuccess(callback);
            } else {
                // 1.2 不是同一个, 先登出旧的
                engine.logout(new Callback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // 1.2.1 再登入新的
                        performLogin(userId, callback);
                    }

                    @Override
                    public void onError(Error error) {
                        // 1.2.2 登出失败, 回调失败信息出去
                        isLoginPending = false;
                        Util.callError(callback, error);
                    }
                });
            }
        } else {
            // 2. 未登录, 直接登录
            performLogin(userId, callback);
        }
    }

    private static void performLogin(final String userId, final Callback<Void> callback) {
        final InteractionEngine engine = InteractionEngine.instance();
        engine.register(new SimpleEngineListener() {
            @Override
            public void onConnectionStatusChanged(DPSConnectionStatus status) {
                if (status == DPSConnectionStatus.CS_AUTHED) {
                    isLoginPending = false;
                    UserHelper.storeUserId(userId);
                    engine.unregister(this);
                    Util.callSuccess(callback);
                }
            }

            @Override
            public void onError(Error error) {
                isLoginPending = false;
                engine.unregister(this);
                Util.callError(callback, error);
            }
        });
        engine.login(userId);
    }
}
