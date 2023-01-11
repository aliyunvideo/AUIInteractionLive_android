package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ContentLoadingProgressBar;

import com.aliyun.aliinteraction.liveroom.live.LiveEvent;
import com.aliyun.aliinteraction.liveroom.live.SimpleLiveEventHandler;
import com.aliyun.aliinteraction.liveroom.BaseComponent;
import com.aliyun.aliinteraction.liveroom.ComponentHolder;
import com.aliyun.aliinteraction.liveroom.IComponent;
import com.aliyun.aliinteraction.liveroom.LiveContext;
import com.aliyun.aliinteraction.liveroom.R;

import java.util.Map;

/**
 * @author puke
 * @version 2022/3/21
 */
public class LiveLoadingView extends FrameLayout implements ComponentHolder {

    public static final String ACTION_SHOW_LOADING = "show_loading";
    public static final String ACTION_HIDE_LOADING = "hide_loading";

    protected final ContentLoadingProgressBar loadingBar;

    private final Component component = new Component();

    public LiveLoadingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setVisibility(GONE);
        inflate(context, R.layout.ilr_view_live_loading, this);
        loadingBar = findViewById(R.id.loading_bar);
    }

    protected void startLoading() {
        setVisibility(VISIBLE);
        loadingBar.show();
    }

    protected void endLoading() {
        setVisibility(GONE);
        loadingBar.hide();
    }

    @Override
    public IComponent getComponent() {
        return component;
    }

    private class Component extends BaseComponent {
        @Override
        public void onInit(LiveContext liveContext) {
            super.onInit(liveContext);

            liveService.addEventHandler(new SimpleLiveEventHandler() {
                @Override
                public void onPusherEvent(LiveEvent event, @Nullable Map<String, Object> extras) {
                    switch (event) {
                        case RECONNECT_START:
                            startLoading();
                            break;
                        // 重新推流成功时, 会回调FIRST_FRAME_PUSHED
                        case FIRST_FRAME_PUSHED:
                        case RECONNECT_SUCCESS:
                        case RECONNECT_FAIL:
                        case CONNECTION_FAIL:
                            endLoading();
                            break;
                    }
                }
            });
        }

        @Override
        public void onEvent(String action, Object... args) {
            switch (action) {
                case ACTION_SHOW_LOADING:
                    startLoading();
                    break;
                case ACTION_HIDE_LOADING:
                    endLoading();
                    break;
            }
        }
    }
}
