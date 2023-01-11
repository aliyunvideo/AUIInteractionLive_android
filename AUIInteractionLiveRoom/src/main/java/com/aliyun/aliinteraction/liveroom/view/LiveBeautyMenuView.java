package com.aliyun.aliinteraction.liveroom.view;

import static com.aliyunsdk.queen.menu.MENU_UI_STYLE.STYLE_LIGHT;

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
import com.aliyunsdk.queen.menu.BeautyMenuPanel;

/**
 * @author puke
 * @version 2021/7/29
 */
public class LiveBeautyMenuView extends FrameLayout implements ComponentHolder {

    private final Component component = new Component();

    public LiveBeautyMenuView(@NonNull Context context) {
        this(context, null, 0);
    }

    public LiveBeautyMenuView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveBeautyMenuView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.irl_beauty, LiveBeautyMenuView.this);
        BeautyMenuPanel beautyMenuPanel = findViewById(R.id.beauty_beauty_menuPanel);
        beautyMenuPanel.onSetMenuUIStyle(STYLE_LIGHT);
        beautyMenuPanel.onHideCopyright();
        beautyMenuPanel.onSetMenuBackgroundResource(R.color.white);
    }

    @Override
    public IComponent getComponent() {
        return component;
    }

    private class Component extends BaseComponent {

        @Override
        public void onEnterRoomSuccess(LiveModel liveModel) {
            super.onEnterRoomSuccess(liveModel);
        }

        @Override
        public void onEvent(String action, Object... args) {
            switch (action) {
                case Actions.BEAUTY_CLICKED:
                    if (getVisibility() == View.VISIBLE) {
                        setVisibility(GONE);
                    } else {
                        setVisibility(VISIBLE);
                    }


            }
        }
    }
}
