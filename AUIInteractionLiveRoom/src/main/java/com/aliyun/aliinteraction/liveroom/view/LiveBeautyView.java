package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aliyun.aliinteraction.core.base.Actions;
import com.aliyun.aliinteraction.liveroom.BaseComponent;
import com.aliyun.aliinteraction.liveroom.ComponentHolder;
import com.aliyun.aliinteraction.liveroom.IComponent;
import com.aliyun.aliinteraction.liveroom.R;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;

public class LiveBeautyView extends FrameLayout implements ComponentHolder {

    private final LiveBeautyView.Component component = new LiveBeautyView.Component();

    public LiveBeautyView(@NonNull Context context) {
        this(context, null, 0);
    }

    public LiveBeautyView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveBeautyView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.ilr_view_live_beauty, this);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                component.handleBeautyClick();
            }
        });
    }

    @Override
    public IComponent getComponent() {
        return component;
    }

    private class Component extends BaseComponent {

        @Override
        public void onEnterRoomSuccess(LiveModel liveModel) {
            setVisibility(isOwner() && !needPlayback() ? VISIBLE : GONE);
        }

        private void handleBeautyClick() {
            // 抛出分享事件
            postEvent(Actions.BEAUTY_CLICKED);
        }
    }
}

