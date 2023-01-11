package com.aliyun.aliinteraction.liveroom.live;

import android.content.Context;

import com.aliyun.aliinteraction.common.base.EventHandlerManager;
import com.aliyun.aliinteraction.liveroom.live.exposable.LiveEventHandler;
import com.aliyun.aliinteraction.liveroom.live.exposable.LivePusherService;
import com.aliyun.aliinteraction.liveroom.live.exposable.LiveService;
import com.aliyun.aliinteraction.liveroom.LiveContext;
import com.aliyun.aliinteraction.liveroom.model.LiveModel;

/**
 * @author puke
 * @version 2021/6/21
 */
public class LiveServiceImpl extends EventHandlerManager<LiveEventHandler> implements LiveService {

    protected final LiveContext liveContext;
    protected final String userId;
    protected final String liveId;
    protected final Context context;

    private final LiveServiceContext serviceContext;

    private LivePusherServiceImpl pusherService;

    public LiveServiceImpl(Context context, LiveContext liveContext) {
        this.liveContext = liveContext;
        this.liveId = liveContext.getLiveId();

        this.userId = liveContext.getUserId();
        this.context = context;

        serviceContext = new LiveServiceContext();
    }

    protected boolean isOwner() {
        return liveContext.isOwner(userId);
    }

    @Override
    public LiveModel getLiveModel() {
        return liveContext.getLiveModel();
    }

    @Override
    public LivePusherService getPusherService() {
        if (pusherService == null) {
            pusherService = new LivePusherServiceImpl(serviceContext, null);
        }
        return pusherService;
    }

    /**
     * 代码重构, 将{@link LivePlayerServiceImpl}和{@link LivePusherServiceImpl}从本类中独立出去<br/>
     * 其中与本类耦合的逻辑, 通过{@link LiveServiceContext}进行桥接
     */
    public class LiveServiceContext {
        public Context getContext() {
            return context;
        }

        public String getUserId() {
            return userId;
        }

        public String getLiveId() {
            return liveId;
        }

        public LiveModel getLiveModel() {
            return LiveServiceImpl.this.getLiveModel();
        }

        public boolean isOwner() {
            return LiveServiceImpl.this.isOwner();
        }

        public void dispatch(Consumer<LiveEventHandler> consumer) {
            LiveServiceImpl.this.dispatch(consumer);
        }
    }
}
