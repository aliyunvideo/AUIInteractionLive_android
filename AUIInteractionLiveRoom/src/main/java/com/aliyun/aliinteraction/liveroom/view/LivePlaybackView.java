package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aliyun.aliinteraction.liveroom.live.SimpleLivePlayerEventHandler;
import com.aliyun.aliinteraction.liveroom.BaseComponent;
import com.aliyun.aliinteraction.liveroom.ComponentHolder;
import com.aliyun.aliinteraction.liveroom.IComponent;
import com.aliyun.aliinteraction.liveroom.LiveContext;
import com.aliyun.aliinteraction.liveroom.R;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;
import com.aliyun.aliinteraction.uikit.uibase.view.ControlView;

/**
 * @author puke
 * @version 2021/11/1
 */
public class LivePlaybackView extends FrameLayout implements ComponentHolder {

    private final Component component = new Component();

    private final ControlView controlView;

    public LivePlaybackView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        View.inflate(context, R.layout.ilr_view_live_playback, this);
        controlView = findViewById(R.id.view_control_view);

        controlView.setPlayStatusClickListener(component);
        controlView.setOnSeekListener(component);
        setVisible(false);
    }

    private void setVisible(boolean visible) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = visible ? LayoutParams.WRAP_CONTENT : 0;
            setLayoutParams(layoutParams);
        }
    }

    @Override
    public IComponent getComponent() {
        return component;
    }

    private class Component extends BaseComponent
            implements ControlView.PlayStatusChange, ControlView.OnSeekListener {

        boolean isEnd;
        boolean isDragging;

        @Override
        public void onInit(LiveContext liveContext) {
            super.onInit(liveContext);
            if (!needPlayback()) {
                return;
            }

            liveContext.getLivePlayerService().addEventHandler(new SimpleLivePlayerEventHandler() {

                @Override
                public void onRenderStart() {
                    controlView.setDuration(getPlayerService().getDuration());
                    controlView.setPlayStatus(true);
                }

                @Override
                public void onPlayerCurrentPosition(long position) {
                    if (!isDragging) {
                        controlView.setCurrentPosition(position);
                        controlView.updateProgress(position);
                    }
                }

                @Override
                public void onPlayerEnd() {
                    controlView.setPlayStatus(false);
                    controlView.setCurrentPosition(0);
                    controlView.updateProgress(0);
                    isEnd = true;
                }
            });
        }

        @Override
        public void onEnterRoomSuccess(LiveModel liveModel) {
            setVisible(needPlayback());
        }

        @Override
        public void onPause() {
            getPlayerService().pausePlay();
        }

        @Override
        public void onResume() {
            if (isEnd) {
                isEnd = false;
                getPlayerService().refreshPlay();
            } else {
                getPlayerService().resumePlay();
            }
        }

        @Override
        public void onSeekStart(int position) {
            isDragging = true;
            getPlayerService().pausePlay();
        }

        @Override
        public void onSeekEnd(int position) {
            isDragging = false;
            getPlayerService().resumePlay();
            getPlayerService().seekTo(position);
        }

        @Override
        public void onProgressChanged(int position) {

        }
    }
}
