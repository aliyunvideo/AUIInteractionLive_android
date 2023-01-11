package com.aliyun.aliinteraction.liveroom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.dingpaas.interaction.ImBroadCastStatistics;
import com.alibaba.dingpaas.interaction.ImGetGroupStatisticsRsp;
import com.aliyun.aliinteraction.core.base.Actions;
import com.aliyun.aliinteraction.liveroom.BaseComponent;
import com.aliyun.aliinteraction.liveroom.ComponentHolder;
import com.aliyun.aliinteraction.liveroom.IComponent;
import com.aliyun.aliinteraction.liveroom.LiveContext;
import com.aliyun.aliinteraction.liveroom.R;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;
import com.aliyun.aliinteraction.model.JoinGroupModel;
import com.aliyun.aliinteraction.model.LikeModel;
import com.aliyun.aliinteraction.model.Message;
import com.aliyun.aliinteraction.roompaas.message.listener.SimpleOnMessageListener;
import com.aliyun.aliinteraction.uikit.uibase.util.AppUtil;

import java.util.Locale;

public class LiveCountInfoView extends FrameLayout implements ComponentHolder {

    private final Component component = new Component();
    private final TextView anchorCount;

    public LiveCountInfoView(@NonNull final Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setMinimumHeight(AppUtil.dp(28));
        setBackgroundResource(R.drawable.ilr_bg_anchor_profile);
        inflate(context, R.layout.ilr_view_live_count_info, this);
        anchorCount = findViewById(R.id.anchor_count);
    }


    /**
     * 设置观看人数
     *
     * @param count 观看人数
     */
    public void setViewCount(int count) {
        anchorCount.setText(formatNumber(count));
    }



    private String formatNumber(int number) {
        if ((number < 0)) {
            // 兜底保护
            return String.valueOf(0);
        } else if (number >= 10000) {
            // 1w+ 格式化
            return String.format(Locale.getDefault(), "%.1fw", number / 10000f);
        } else {
            return String.valueOf(number);
        }
    }

    @Override
    public IComponent getComponent() {
        return component;
    }

    private class Component extends BaseComponent {

        @Override
        public void onInit(LiveContext liveContext) {
            super.onInit(liveContext);

            // 监听互动信息变化
            getMessageService().addMessageListener(new SimpleOnMessageListener() {
                @Override
                public void onJoinGroup(Message<JoinGroupModel> message) {
                    ImBroadCastStatistics statistics = message.data.statistics;
                    if (statistics != null) {
                        setViewCount(statistics.pv);
                    }
                }

                @Override
                public void onLikeReceived(Message<LikeModel> message) {
                   // setLikeCount(message.data.likeCount);
                }
            });
        }

        @Override
        public void onEnterRoomSuccess(LiveModel liveModel) {
            // 进入房间后, 填充房间基本信息
            setViewCount(liveModel.pv);
        }

        @Override
        public void onEvent(String action, Object... args) {
            if (Actions.GET_GROUP_STATISTICS_SUCCESS.equals(action)) {
                if (args.length > 0 && args[0] instanceof ImGetGroupStatisticsRsp) {
                    ImGetGroupStatisticsRsp rsp = (ImGetGroupStatisticsRsp) args[0];
                    setViewCount(rsp.pv);
                }
            }
        }
    }
}
