package com.aliyun.aliinteraction.liveroom.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.aliyun.aliinteraction.InteractionEngine;
import com.aliyun.aliinteraction.base.AppContext;
import com.aliyun.aliinteraction.base.Callback;
import com.aliyun.aliinteraction.base.Error;
import com.aliyun.aliinteraction.error.Errors;
import com.aliyun.aliinteraction.liveroom.util.AppUtil;
import com.aliyun.aliinteraction.util.Util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * @author puke
 * @version 2022/8/31
 */
class ApiInvokerCallAdapterFactory extends CallAdapter.Factory {

    @Override
    public CallAdapter<?, ?> get(@NonNull Type returnType, @NonNull Annotation[] annotations,
                                 @NonNull Retrofit retrofit) {
        if (returnType instanceof ParameterizedType
                && ((ParameterizedType) returnType).getRawType() == ApiInvoker.class) {
            Type responseType = getParameterUpperBound(0, (ParameterizedType) returnType);
            return new CallAdapterForApiInvoker<>(responseType);
        }
        return null;
    }

    private static class CallAdapterForApiInvoker<R> implements CallAdapter<R, ApiInvoker<R>> {

        final Type responseType;

        CallAdapterForApiInvoker(Type responseType) {
            this.responseType = responseType;
        }

        @NonNull
        @Override
        public Type responseType() {
            return responseType;
        }

        @NonNull
        @Override
        public ApiInvoker<R> adapt(@NonNull final Call<R> call) {
            return new ApiInvoker<R>() {
                @Override
                public void invoke(final Callback<R> callback) {
                    call.enqueue(new retrofit2.Callback<R>() {
                        @Override
                        public void onResponse(@NonNull Call<R> call, @NonNull Response<R> response) {
                            int httpCode = response.code();
                            switch (httpCode) {
                                case 200:
                                    R body = response.body();
                                    Util.callSuccess(callback, body);
                                    break;
                                case 401:
                                    if (InteractionEngine.instance().isLogin()) {
                                        // AppServer的token过期
                                        AppServerTokenManager.refreshToken(new Callback<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                // 刷新token后再次请求
                                                invoke(callback);
                                            }

                                            @Override
                                            public void onError(Error error) {
                                                Util.callError(callback, error);
                                            }
                                        });
                                    } else {
                                        // 登录失败
                                        Util.callError(callback, Errors.BIZ_ERROR, "用户名或密码错误");
                                    }
                                    break;
                                default:
                                    String msg = "http code is " + httpCode;
                                    ResponseBody responseBody = response.errorBody();
                                    if (responseBody != null) {
                                        try {
                                            String serverMsg = responseBody.string();
                                            if (!TextUtils.isEmpty(serverMsg)) {
                                                msg = serverMsg;
                                            }
                                        } catch (IOException ignored) {
                                        }
                                    }
                                    Util.callError(callback, Errors.BIZ_ERROR, msg);
                                    break;
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<R> call, @NonNull Throwable t) {
                            if (AppUtil.isNetworkInvalid(AppContext.getContext())) {
                                Util.callError(callback, Errors.BIZ_ERROR, "当前网络不可用，请检查后再试");
                            } else {
                                Util.callError(callback, Errors.BIZ_ERROR, t.getMessage());
                            }
                        }
                    });
                }
            };
        }
    }
}
