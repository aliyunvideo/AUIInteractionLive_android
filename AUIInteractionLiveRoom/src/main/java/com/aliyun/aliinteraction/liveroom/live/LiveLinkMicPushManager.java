package com.aliyun.aliinteraction.liveroom.live;

import static android.os.Environment.MEDIA_MOUNTED;
import static com.alivc.live.pusher.AlivcLivePushCameraTypeEnum.CAMERA_TYPE_BACK;
import static com.alivc.live.pusher.AlivcLivePushCameraTypeEnum.CAMERA_TYPE_FRONT;

import android.content.Context;
import android.hardware.Camera;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alivc.component.custom.AlivcLivePushCustomFilter;
import com.alivc.live.annotations.AlivcLiveMode;
import com.aliyun.aliinteraction.beauty.BeautyFactory;
import com.aliyun.aliinteraction.beauty.BeautyInterface;
import com.aliyun.aliinteraction.beauty.constant.BeautySDKType;
import com.alivc.live.player.AlivcLivePlayConfig;
import com.alivc.live.player.AlivcLivePlayInfoListener;
import com.alivc.live.player.AlivcLivePlayer;
import com.alivc.live.player.annotations.AlivcLivePlayError;
import com.alivc.live.pusher.AlivcAudioAACProfileEnum;
import com.alivc.live.pusher.AlivcEncodeModeEnum;
import com.alivc.live.pusher.AlivcFpsEnum;
import com.alivc.live.pusher.AlivcLiveBase;
import com.alivc.live.pusher.AlivcLiveBaseListener;
import com.alivc.live.pusher.AlivcLiveMixStream;
import com.alivc.live.pusher.AlivcLivePushConfig;
import com.alivc.live.pusher.AlivcLivePushConstants;
import com.alivc.live.pusher.AlivcLivePushError;
import com.alivc.live.pusher.AlivcLivePushErrorListener;
import com.alivc.live.pusher.AlivcLivePushInfoListener;
import com.alivc.live.pusher.AlivcLivePushNetworkListener;
import com.alivc.live.pusher.AlivcLivePushStats;
import com.alivc.live.pusher.AlivcLivePushStatsInfo;
import com.alivc.live.pusher.AlivcLivePusher;
import com.alivc.live.pusher.AlivcLiveTranscodingConfig;
import com.alivc.live.pusher.AlivcPreviewDisplayMode;
import com.alivc.live.pusher.AlivcPreviewOrientationEnum;
import com.aliyun.aliinteraction.common.base.log.Logger;
import com.aliyun.aliinteraction.common.base.util.CollectionUtil;
import com.aliyun.aliinteraction.core.utils.AssetUtil;
import com.aliyun.aliinteraction.liveroom.linkmic.InteractLivePushPullListener;
import com.aliyun.aliinteraction.liveroom.linkmic.LivePushGlobalConfig;
import com.aliyun.aliinteraction.liveroom.linkmic.MultiAlivcLivePlayer;
import com.aliyun.aliinteraction.liveroom.linkmic.MultiInteractLivePushPullListener;
import com.aliyun.aliinteraction.player.exposable.CanvasScale;
import com.aliyun.aliinteraction.uikit.uibase.util.AppUtil;
import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;
import com.aliyun.player.source.UrlSource;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 媒体推流服务
 */
public class LiveLinkMicPushManager implements AlivcLiveBaseListener {

    private static final String TAG = LiveLinkMicPushManager.class.getSimpleName();
    private static final String LIVE_PUSH_CONFIG_EXTRA_INFO = "channel_aliyun_solution";

    private AlivcLivePusher mALivcLivePusher;
    private AliLivePusherOptions mAliLivePusherOptions;
    private SurfaceView mSurfaceView;

    private boolean isStartPush = false;
    private boolean isPause = false;

    // 当前直播推流地址
    private String mCurrentPublishUrl;
    private final Context mContext;

    private Callback callback;
    private String queenSecret;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private SurfaceHolder.Callback surfaceViewCallback;
    private InteractLivePushPullListener mInteractLivePushPullListener;
    private boolean mHasPulled = false;
    private boolean mHasPushed = false;
    private FrameLayout renderView;
    private AliPlayer mAliPlayer;
    private MultiAlivcLivePlayer mAlivcLivePlayer;
    private boolean mIsPlaying = false;
    private final Map<String, MultiAlivcLivePlayer> mAlivcLivePlayerMap = new HashMap<>();
    private String mPushUrl;
    private String mPullUrl;
    public static final String ARTC = "artc://";
    private FrameLayout mAudienceFrameLayout;
    private AlivcLivePushConfig mAlivcLivePushConfig;
    //多人连麦混流
    private final ArrayList<AlivcLiveMixStream> mMultiInteractLiveMixStreamsArray = new ArrayList<>();
    //多人连麦 Config
    private final AlivcLiveTranscodingConfig mMixInteractLiveTranscodingConfig = new AlivcLiveTranscodingConfig();
    private MultiInteractLivePushPullListener mMultiInteractLivePushPullListener;
    private final int mAudiencelayoutArgs = 180;//设置布局样式，先默认180dp
    private BeautyInterface mBeautyManager;
    private boolean isBeautyEnable = true;


    public LiveLinkMicPushManager(Context context, AliLivePusherOptions aliLivePusherOptions) {
        mContext = context;
        mAliLivePusherOptions = aliLivePusherOptions;
        init();
        initPlayer();
    }

    private void init() {
        if (mALivcLivePusher == null) {
            mALivcLivePusher = new AlivcLivePusher();
        }
        // 注册sdk
        AlivcLiveBase.setListener(this);
        AlivcLiveBase.registerSDK();

        // 初始化推流配置类
        mAlivcLivePushConfig = new AlivcLivePushConfig();
        mAlivcLivePushConfig.setPreviewDisplayMode(AlivcPreviewDisplayMode.ALIVC_LIVE_PUSHER_PREVIEW_ASPECT_FILL);
        mAlivcLivePushConfig.setLivePushMode(AlivcLiveMode.AlivcLiveInteractiveMode);
        // 分辨率540P，最大支持720P
//        mAlivcLivePushConfig.setResolution(AlivcResolutionEnum.RESOLUTION_540P);
        mAlivcLivePushConfig.setResolution(LivePushGlobalConfig.CONFIG_RESOLUTION);
        // 建议用户使用20fps
        mAlivcLivePushConfig.setFps(AlivcFpsEnum.FPS_20);
        // 打开码率自适应，默认为true
        mAlivcLivePushConfig.setEnableBitrateControl(true);
        // 默认为竖屏，可设置home键向左或向右横屏
        mAlivcLivePushConfig.setPreviewOrientation(AlivcPreviewOrientationEnum.ORIENTATION_PORTRAIT);
        // 设置音频编码模式
        mAlivcLivePushConfig.setAudioProfile(AlivcAudioAACProfileEnum.AAC_LC);
        mAlivcLivePushConfig.setVideoEncodeMode(LivePushGlobalConfig.VIDEO_ENCODE_HARD ? AlivcEncodeModeEnum.Encode_MODE_HARD : AlivcEncodeModeEnum.Encode_MODE_SOFT);
        mAlivcLivePushConfig.setAudioEncodeMode(LivePushGlobalConfig.AUDIO_ENCODE_HARD ? AlivcEncodeModeEnum.Encode_MODE_HARD : AlivcEncodeModeEnum.Encode_MODE_SOFT);
        mAlivcLivePushConfig.setPausePushImage(getPushPauseImagePath());
        mALivcLivePusher = new AlivcLivePusher();
        mALivcLivePusher.init(mContext, mAlivcLivePushConfig);
        mALivcLivePusher.setLivePushErrorListener(new AlivcLivePushErrorListener() {
            @Override
            public void onSystemError(AlivcLivePusher alivcLivePusher, AlivcLivePushError alivcLivePushError) {
                Log.d(TAG, "onSystemError: ");
                if (mInteractLivePushPullListener != null) {
                    mInteractLivePushPullListener.onPushError();
                }
            }

            @Override
            public void onSDKError(AlivcLivePusher alivcLivePusher, AlivcLivePushError alivcLivePushError) {
                Log.d(TAG, "onSDKError: ");
                if (mInteractLivePushPullListener != null) {
                    mInteractLivePushPullListener.onPushError();
                }
            }
        });
        mALivcLivePusher.setLivePushInfoListener(pushInfoListener);
        mALivcLivePusher.setLivePushNetworkListener(pushNetworkListener);
        mALivcLivePusher.setLivePushErrorListener(pushErrorListener);

        // mALivcLivePusher.setCustomFilter();
        mALivcLivePusher.setCustomFilter(new AlivcLivePushCustomFilter() {
            @Override
            public void customFilterCreate(long var1) {
                Log.d(TAG, "customFilterCreate start-" + var1);

                initBeautyManager(var1);

                Log.d(TAG, "customFilterCreate end");
            }

            @Override
            public int customFilterProcess(int inputTexture, int textureWidth, int textureHeight, float[] textureMatrix, boolean isOES, long extra) {
                if (mBeautyManager == null) {
                    return inputTexture;
                }

                int ret = inputTexture;
                if (mAlivcLivePushConfig != null && mAlivcLivePushConfig.getLivePushMode() == AlivcLiveMode.AlivcLiveInteractiveMode) {
                    ret = mBeautyManager.onTextureInput(inputTexture, textureWidth, textureHeight, textureMatrix, isOES);
                } else {
                    ret = mBeautyManager.onTextureInput(inputTexture, textureWidth, textureHeight, textureMatrix, false);
                }
                return ret;
            }

            @Override
            public void customFilterDestroy() {
                destroyBeautyManager();
                Log.d(TAG, "customFilterDestroy---> thread_id: " + Thread.currentThread().getId());
            }
        });

    }

    @NonNull
    private String getPushPauseImagePath() {
        // 此处文件名与 AUIInteractionLiveRoom/src/main/assets/background_push.png 对应
        String targetFileName = "background_push.png";
        File imageFile = new File(mContext.getFilesDir(), targetFileName);
        String imageFilePath = imageFile.getAbsolutePath();
        if (!imageFile.exists()) {
            AssetUtil.copyAssetFileToSdCard(mContext, targetFileName, imageFilePath);
        }
        return imageFilePath;
    }

    public static String getFilePath(Context context, String dir) {
        String logFilePath = "";
        //判断SD卡是否可用
        if (MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            logFilePath = context.getExternalFilesDir(dir).getAbsolutePath();
        } else {
            //没内存卡就存机身内存
            logFilePath = context.getFilesDir() + File.separator + dir;
        }
        File file = new File(logFilePath);
        if (!file.exists()) {//判断文件目录是否存在
            file.mkdirs();
        }

        //Set log folder path in 4.4.0+ version
        if (false) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String logFileName = "live_pusher_" + sdf.format(new Date()) + "_" + String.valueOf(System.currentTimeMillis()) + ".log";
            logFilePath += File.separator + logFileName;
        }

        Log.d(TAG, "log filePath====>" + logFilePath);
        return logFilePath;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    protected void onEvent(LiveEvent event) {
        onEvent(event, null);
    }

    protected void onEvent(LiveEvent event, Map<String, Object> extras) {
        if (callback != null) {
            callback.onEvent(event, extras);
        }
    }

    /**
     * 切换闪光灯
     *
     * @param open 是否开启
     */
    public void setFlash(boolean open) {
        if (mALivcLivePusher != null && isPreviewedOrPushed()) {
            mALivcLivePusher.setFlash(open);
        }
    }

    /**
     * @return 判断是否支持闪光灯
     */
    public boolean isCameraSupportFlash() {
        return mALivcLivePusher != null && mALivcLivePusher.isCameraSupportFlash();
    }

    /**
     * @param zoom
     */
    public void setZoom(int zoom) {
        if (mALivcLivePusher != null && isPreviewedOrPushed()) {
            mALivcLivePusher.setZoom(zoom);
        }
    }

    public int getCurrentZoom() {
        if (mALivcLivePusher != null && isPreviewedOrPushed()) {
            return mALivcLivePusher.getCurrentZoom();
        }
        return 0;
    }

    public int getMaxZoom() {
        if (mALivcLivePusher != null && isPreviewedOrPushed()) {
            return mALivcLivePusher.getMaxZoom();
        }
        return 0;
    }

    private boolean isPreviewedOrPushed() {
        AlivcLivePushStats pushStats = mALivcLivePusher.getCurrentStatus();
        return pushStats == AlivcLivePushStats.PREVIEWED || pushStats == AlivcLivePushStats.PUSHED;
    }

    private void resumePublish() {
        if (TextUtils.isEmpty(mCurrentPublishUrl)) {
            Logger.w(TAG, "resumePublish publishUrl is empty");
            return;
        }
        if (mALivcLivePusher == null) {
            Logger.w(TAG, "resumePublish mALivcLivePusher is null");
            return;
        }

        AlivcLivePushStats currentStatus = mALivcLivePusher.getCurrentStatus();
        if (currentStatus == AlivcLivePushStats.PUSHED) {
            Logger.w(TAG, "resumePublish currentStatus is already pushed");
            return;
        }

//        if (mSurfaceView == null) {
//            Logger.w(TAG, "resumePublish mSurfaceView is null");
//            return;
//        }

        // 未预览时, 先开启预览
        if (currentStatus != AlivcLivePushStats.PREVIEWED) {
            if (currentStatus == AlivcLivePushStats.INIT) {
                Logger.i(TAG, "resumePublish start preview");
                doStartPreviewByMediaSdk(mSurfaceView);
            } else {
                Logger.w(TAG, "resumePublish currentStatus is error");
                return;
            }
        }

        // 开始推流
        Logger.i(TAG, "resumePublish start publish");
        startPublish(mCurrentPublishUrl);
    }

    /**
     * 开启推流
     *
     * @param url
     * @return
     */
    public SurfaceView startPublish(@NonNull String url) {
        if (url == null || url.length() == 0) {
            Logger.e(TAG, "startPublish url must not null");
            return null;
        }
        if (isStartPush
                || (mALivcLivePusher.isPushing() && url.equals(mALivcLivePusher.getPushUrl()))) {
            Logger.i(TAG, "startPublish url is same");
            return mSurfaceView;
        }
        if (mALivcLivePusher.isPushing()) {
            mALivcLivePusher.stopPush();
        }
        mCurrentPublishUrl = url;
        mALivcLivePusher.startPushAysnc(url);
        isStartPush = true;
        return mSurfaceView;
    }

    /**
     * 获取当前推流地址
     *
     * @return
     */
    public String getCurrentPublishUrl() {
        return mCurrentPublishUrl;
    }


    /**
     * 暂停
     */
    public void pause() {
        if (mALivcLivePusher != null) {
            try {
                mALivcLivePusher.pause();
            } catch (Exception e) {
                Logger.e(TAG, "pause error", e);
            }
            isStartPush = false;
        }
    }

    /**
     * 继续
     */
    public void resume() {
        if (mALivcLivePusher != null) {
            try {
                mALivcLivePusher.resume();
            } catch (Exception e) {
                Logger.e(TAG, "resume error", e);
            }
            isStartPush = true;
        }
    }

    /**
     * 结束推流
     */
    public void stopPublish() {
        if (mALivcLivePusher != null && mALivcLivePusher.isPushing()) {
            try {
                mALivcLivePusher.stopPush();
            } catch (Exception e) {
                Logger.e(TAG, "stopPublish error", e);
            }
            isStartPush = false;
//            mCurrentPublishUrl = null;
        }
        stopPreview();
    }

    private SurfaceView createSurfaceView() {
        final SurfaceView surfaceView = new SurfaceView(mContext);
        surfaceViewCallback = new SurfaceHolder.Callback() {

            private AlivcLivePushStats lastStatus;

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (mALivcLivePusher == null) {
                    return;
                }

                AlivcLivePushStats currentStatus = mALivcLivePusher.getCurrentStatus();
                Logger.i(TAG, String.format("surfaceCreated, lastStatus is %s, currentStatus is %s",
                        lastStatus, currentStatus));

                if (isPause) {
                    // 暂停状态不继续预览和推流
                    Logger.i(TAG, "surfaceCreated, pusher is paused");
                    return;
                }
                doStartPreviewByMediaSdk(surfaceView);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                lastStatus = mALivcLivePusher == null ? null : mALivcLivePusher.getCurrentStatus();
                Logger.i(TAG, String.format("surfaceDestroyed, lastStatus is %s", lastStatus));
            }
        };
        surfaceView.getHolder().addCallback(surfaceViewCallback);
        return surfaceView;
    }

    public void setPreviewMode(AlivcPreviewDisplayMode mode) {
        try {
            mALivcLivePusher.setPreviewMode(mode);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void doStartPreviewByMediaSdk(SurfaceView surfaceView) {
        if (mALivcLivePusher != null) {
            try {
                //mALivcLivePusher.startPreview(surfaceView);
//                FrameLayout framelayout = new FrameLayout(mContext);
//                framelayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
//                        FrameLayout.LayoutParams.MATCH_PARENT));
                mALivcLivePusher.startPreview(mContext, renderView, false);
            } catch (IllegalArgumentException | IllegalStateException e) {
                Logger.e(TAG, "surface create error", e);
            }
        }
    }

    public void setViewContentMode(@CanvasScale.Mode int mode) {
        if (mALivcLivePusher != null) {
            mALivcLivePusher.setPreviewMode(convertToAlivcPreviewDisplayMode(mode));
        }
    }

    public static AlivcPreviewDisplayMode convertToAlivcPreviewDisplayMode(@CanvasScale.Mode int mode) {
        switch (mode) {
            case CanvasScale.Mode.SCALE_FILL:
                return AlivcPreviewDisplayMode.ALIVC_LIVE_PUSHER_PREVIEW_SCALE_FILL;
            case CanvasScale.Mode.ASPECT_FIT:
                return AlivcPreviewDisplayMode.ALIVC_LIVE_PUSHER_PREVIEW_ASPECT_FIT;
            default:
            case CanvasScale.Mode.ASPECT_FILL:
                return AlivcPreviewDisplayMode.ALIVC_LIVE_PUSHER_PREVIEW_ASPECT_FILL;
        }
    }

    /**
     * 开始预览
     */
    public View startPreview(FrameLayout renderView) {
        this.renderView = renderView;
        if (mALivcLivePusher == null) {
            init();
        }

        doStartPreviewByMediaSdk(mSurfaceView);

        return renderView;
    }

    private void stopPreview() {
        if (mALivcLivePusher != null) {
            try {
                mALivcLivePusher.stopPreview();
            } catch (Exception e) {
                Logger.e(TAG, "stopPreview error", e);
            }
        }
    }

    /**
     * 获取预览View
     *
     * @return
     */
    public SurfaceView getAliLiveRenderView() {
        return mSurfaceView;
    }

    /**
     * 销毁直播服务
     */
    public void destroy() {
        stopPublish();

        if (mALivcLivePusher != null) {
            mALivcLivePusher.destroy();
            mALivcLivePusher = null;
        }
    }

    /**
     * 静音播放
     */
    public void setMute(boolean mute) {
        mALivcLivePusher.setMute(mute);
    }


    private void resumeScreenCapture() {
        try {
            mALivcLivePusher.resumeScreenCapture();
        } catch (Throwable e) {
            Logger.e(TAG, "resumeScreenCapture: error:", e);
        }
    }

    private void pauseScreenCapture() {
        try {
            mALivcLivePusher.pauseScreenCapture();
        } catch (Throwable e) {
            Logger.e(TAG, "pauseScreenCapture: error:", e);
        }
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
//        if (mALivcLivePusher == null || isPushDisable()) {
//            return;
//        }
        if (mCameraId == CAMERA_TYPE_FRONT.getCameraId()) {
            mCameraId = CAMERA_TYPE_BACK.getCameraId();
        } else {
            mCameraId = CAMERA_TYPE_FRONT.getCameraId();
        }
        mALivcLivePusher.switchCamera();

//        if (mBeautyManager != null) {
//            mBeautyManager.switchCameraId(mCameraId);
//        }
    }

    /**
     * 设置预览镜像
     *
     * @param mirror
     */
    public void setPreviewMirror(boolean mirror) {
        if (mALivcLivePusher == null) {
            return;
        }
        mALivcLivePusher.setPreviewMirror(mirror);
    }

    /**
     * 设置推流镜像
     *
     * @param mirror
     */
    public void setPushMirror(boolean mirror) {
        if (mALivcLivePusher == null) {
            return;
        }
        mALivcLivePusher.setPushMirror(mirror);
    }

    protected void setQueenSecret(String secret) {
        this.queenSecret = secret;
    }

    /**
     * 重新推流
     * 推流状态下或者接收到所有Error相关回调状态下可调用重新推流, 且Error状态下只可以调用此接口(或者reconnectPushAsync重连)或者调用destory销毁推流。
     */
    public void restartPush() {
        if (mALivcLivePusher == null) {
            return;
        }
        try {
            mALivcLivePusher.restartPush();
        } catch (IllegalArgumentException | IllegalStateException e) {
            Logger.e(TAG, "restartPush error", e);
        }
    }

    /**
     * 推流状态下或者接收到AlivcLivePusherNetworkDelegate相关的Error回调状态下可调用此接口,
     * 且Error状态下只可以调用此接口(或者restartPush重新推流)或者调用destory销毁推流。完成后推流重连，重新链接推流RTMP
     */
    public void reconnectPushAsync() {
        if (mALivcLivePusher == null) {
            return;
        }
        try {
            mALivcLivePusher.reconnectPushAsync(mCurrentPublishUrl);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Logger.e(TAG, "restartPush error", e);
        }
    }

    // region listener
    AlivcLivePushInfoListener pushInfoListener = new AlivcLivePushInfoListener() {

        @Override
        public void onPreviewStarted(AlivcLivePusher alivcLivePusher) {
            onEvent(LiveEvent.PREVIEW_STARTED);
        }

        @Override
        public void onPreviewStoped(AlivcLivePusher alivcLivePusher) {
            onEvent(LiveEvent.PREVIEW_STOPPED);
        }

        @Override
        public void onPushStarted(AlivcLivePusher alivcLivePusher) {
            onEvent(LiveEvent.PUSH_STARTED);
        }

        @Override
        public void onFirstAVFramePushed(AlivcLivePusher alivcLivePusher) {
            onEvent(LiveEvent.FIRST_FRAME_PUSHED);
        }

        @Override
        public void onPushPauesed(AlivcLivePusher alivcLivePusher) {
            onEvent(LiveEvent.PUSH_PAUSED);
        }

        @Override
        public void onPushResumed(AlivcLivePusher alivcLivePusher) {
            onEvent(LiveEvent.PUSH_RESUMED);
        }

        @Override
        public void onPushStoped(AlivcLivePusher alivcLivePusher) {
            onEvent(LiveEvent.PUSH_STOPPED);
        }

        @Override
        public void onPushRestarted(AlivcLivePusher alivcLivePusher) {

        }

        @Override
        public void onFirstFramePreviewed(AlivcLivePusher alivcLivePusher) {
            onEvent(LiveEvent.FIRST_FRAME_PREVIEWED);
        }

        @Override
        public void onDropFrame(AlivcLivePusher alivcLivePusher, int i, int i1) {

        }

        @Override
        public void onAdjustBitRate(AlivcLivePusher alivcLivePusher, int curBr, int targetBr) {
        }

        @Override
        public void onAdjustFps(AlivcLivePusher alivcLivePusher, int curFps, int targetFps) {
        }

        @Override
        public void onPushStatistics(AlivcLivePusher alivcLivePusher, AlivcLivePushStatsInfo info) {
            Map<String, Object> extras = new HashMap<>();
            extras.put("v_bitrate", info.getVideoUploadBitrate());
            extras.put("a_bitrate", info.getAudioUploadBitrate());
            onEvent(LiveEvent.UPLOAD_BITRATE_UPDATED, extras);
        }

        @Override
        public void onSetLiveMixTranscodingConfig(AlivcLivePusher alivcLivePusher, boolean b, String s) {

        }
    };

    AlivcLivePushNetworkListener pushNetworkListener = new AlivcLivePushNetworkListener() {
        @Override
        public void onNetworkPoor(AlivcLivePusher pusher) {
            onEvent(LiveEvent.NETWORK_POOR);
        }

        @Override
        public void onNetworkRecovery(AlivcLivePusher pusher) {
            onEvent(LiveEvent.NETWORK_RECOVERY);
        }

        @Override
        public void onReconnectStart(AlivcLivePusher pusher) {
            onEvent(LiveEvent.RECONNECT_START);
        }

        @Override
        public void onReconnectFail(AlivcLivePusher pusher) {
            onEvent(LiveEvent.RECONNECT_FAIL);
        }

        @Override
        public void onReconnectSucceed(AlivcLivePusher pusher) {
            onEvent(LiveEvent.RECONNECT_SUCCESS);
        }

        @Override
        public void onSendDataTimeout(AlivcLivePusher pusher) {
        }

        @Override
        public void onConnectFail(AlivcLivePusher pusher) {
            onEvent(LiveEvent.CONNECTION_FAIL);
        }

        @Override
        public void onConnectionLost(AlivcLivePusher pusher) {
            //推流已断开
            onEvent(LiveEvent.CONNECTION_LOST);
        }

        @Override
        public String onPushURLAuthenticationOverdue(AlivcLivePusher pusher) {
            if (pusher != null) {
                return pusher.getPushUrl();
            }
            return null;
        }

        @Override
        public void onSendMessage(AlivcLivePusher pusher) {
        }

        @Override
        public void onPacketsLost(AlivcLivePusher pusher) {
        }
    };

    AlivcLivePushErrorListener pushErrorListener = new AlivcLivePushErrorListener() {

        @Override
        public void onSystemError(AlivcLivePusher livePusher, AlivcLivePushError error) {
            if (error != null) {
                Logger.e(TAG, error.toString());
                reportPushLowPerformance(error);
            }
        }

        @Override
        public void onSDKError(AlivcLivePusher livePusher, AlivcLivePushError error) {
            if (error != null) {
                Logger.e(TAG, error.toString());
                reportPushLowPerformance(error);
            }
        }
    };

    private void reportPushLowPerformance(AlivcLivePushError error) {
        if (error.getCode() == AlivcLivePushError.ALIVC_PUSHER_ERROR_SDK_LIVE_PUSH_LOW_PERFORMANCE.getCode()) {
        }
    }

    public void focusCameraAtAdjustedPoint(float x, float y, boolean autoFocus) {
        if (mALivcLivePusher != null) {
            try {
                mALivcLivePusher.focusCameraAtAdjustedPoint(x, y, autoFocus);
            } catch (Exception e) {
                Logger.e(TAG, "focusCameraAtAdjustedPoint error", e);
            }
        }
    }

    @Override
    public void onLicenceCheck(AlivcLivePushConstants.AlivcLiveLicenseCheckResultCode alivcLiveLicenseCheckResultCode, String s) {

    }
    // endregion listener

    public interface Callback {

        void onEvent(LiveEvent event, @Nullable Map<String, Object> extras);
    }


    private void initPlayer() {
        mAliPlayer = AliPlayerFactory.createAliPlayer(mContext);
        mAliPlayer.setAutoPlay(true);
        mAlivcLivePlayer = new MultiAlivcLivePlayer(mContext, AlivcLiveMode.AlivcLiveInteractiveMode);
        mAlivcLivePlayer.setPlayInfoListener(new AlivcLivePlayInfoListener() {
            @Override
            public void onPlayStarted() {
                Log.d(TAG, "onPlayStarted: ");
                mIsPlaying = true;
                if (mInteractLivePushPullListener != null) {
                    mInteractLivePushPullListener.onPullSuccess();
                }
            }

            @Override
            public void onPlayStopped() {
                Log.d(TAG, "onPlayStopped: ");
                mIsPlaying = false;
                if (mInteractLivePushPullListener != null) {
                    mInteractLivePushPullListener.onPullStop();
                }
            }

            @Override
            public void onFirstVideoFrameDrawn() {
                Log.d(TAG, "onPlaying: ");
                mHasPulled = true;
            }

            @Override
            public void onError(AlivcLivePlayError alivcLivePlayError, String s) {
                Log.d(TAG, "onError: ");
                mIsPlaying = false;
                if (mInteractLivePushPullListener != null) {
                    mInteractLivePushPullListener.onPullError(alivcLivePlayError, s);
                }
            }
        });

    }

    public void setPullView(FrameLayout frameLayout, boolean isAnchor) {
        this.mAudienceFrameLayout = frameLayout;
        AlivcLivePlayConfig config = new AlivcLivePlayConfig();
        config.isFullScreen = isAnchor;
        mAlivcLivePlayer.setupWithConfig(config);
        mAlivcLivePlayer.setPlayView(frameLayout);
    }

    public void setLiveMixTranscodingConfig(String anchorId, String audience) {
        if (TextUtils.isEmpty(anchorId) && TextUtils.isEmpty(audience)) {
            if (mALivcLivePusher != null) {
                mALivcLivePusher.setLiveMixTranscodingConfig(null);
            }
            return;
        }
        AlivcLiveTranscodingConfig transcodingConfig = new AlivcLiveTranscodingConfig();
        AlivcLiveMixStream anchorMixStream = new AlivcLiveMixStream();
        if (mAlivcLivePushConfig != null) {
            anchorMixStream.setUserId(anchorId);
            anchorMixStream.setX(0);
            anchorMixStream.setY(0);
            anchorMixStream.setWidth(mAlivcLivePushConfig.getWidth());
            anchorMixStream.setHeight(mAlivcLivePushConfig.getHeight());
            anchorMixStream.setZOrder(1);
            Log.d(TAG, "AlivcRTC anchorMixStream --- " + anchorMixStream.getUserId() + ", " + anchorMixStream.getWidth() + ", " + anchorMixStream.getHeight()
                    + ", " + anchorMixStream.getX() + ", " + anchorMixStream.getY() + ", " + anchorMixStream.getZOrder());
        }
        AlivcLiveMixStream audienceMixStream = new AlivcLiveMixStream();
        if (mAudienceFrameLayout != null) {
            audienceMixStream.setUserId(audience);
            audienceMixStream.setX((int) mAudienceFrameLayout.getX() / 3);
            audienceMixStream.setY((int) mAudienceFrameLayout.getY() / 3);
            audienceMixStream.setWidth(mAudienceFrameLayout.getWidth() / 2);
            audienceMixStream.setHeight(mAudienceFrameLayout.getHeight() / 2);

            audienceMixStream.setZOrder(2);
            Log.d(TAG, "AlivcRTC audienceMixStream --- " + audienceMixStream.getUserId() + ", " + audienceMixStream.getWidth() + ", " + audienceMixStream.getHeight()
                    + ", " + audienceMixStream.getX() + ", " + audienceMixStream.getY() + ", " + audienceMixStream.getZOrder());
        }
        ArrayList<AlivcLiveMixStream> mixStreams = new ArrayList<>();
        mixStreams.add(anchorMixStream);
        mixStreams.add(audienceMixStream);
        transcodingConfig.setMixStreams(mixStreams);
        if (mALivcLivePusher != null) {
            mALivcLivePusher.setLiveMixTranscodingConfig(transcodingConfig);
        }
    }

    public void addAnchorMixTranscodingConfig(String anchorId) {
        if (TextUtils.isEmpty(anchorId)) {
            if (mALivcLivePusher != null) {
                mALivcLivePusher.setLiveMixTranscodingConfig(null);
            }
            return;
        }

        AlivcLiveMixStream anchorMixStream = new AlivcLiveMixStream();
        if (mAlivcLivePushConfig != null) {
            anchorMixStream.setUserId(anchorId);
            anchorMixStream.setX(0);
            anchorMixStream.setY(0);
            anchorMixStream.setWidth(mAlivcLivePushConfig.getWidth());
            anchorMixStream.setHeight(mAlivcLivePushConfig.getHeight());
            anchorMixStream.setZOrder(1);
        }

        mMultiInteractLiveMixStreamsArray.add(anchorMixStream);
        mMixInteractLiveTranscodingConfig.setMixStreams(mMultiInteractLiveMixStreamsArray);

        if (mALivcLivePusher != null) {
            mALivcLivePusher.setLiveMixTranscodingConfig(mMixInteractLiveTranscodingConfig);
        }
    }

    public boolean isPushing() {
        return mALivcLivePusher.isPushing();
    }

    public boolean isPulling() {
        return mIsPlaying;
    }

    public void setPullView(String key, FrameLayout frameLayout, boolean isAnchor) {
        AlivcLivePlayConfig config = new AlivcLivePlayConfig();
        config.isFullScreen = isAnchor;
        AlivcLivePlayer alivcLivePlayer = mAlivcLivePlayerMap.get(key);
        if (alivcLivePlayer != null) {
            alivcLivePlayer.setupWithConfig(config);
            alivcLivePlayer.setPlayView(frameLayout);
        }
    }

    public void setPullView(SurfaceHolder surfaceHolder) {
        mAliPlayer.setDisplay(surfaceHolder);
    }

    public void startPull(String key, String url) {
        this.mPullUrl = url;
        if (url.startsWith(ARTC)) {
            AlivcLivePlayer alivcLivePlayer = mAlivcLivePlayerMap.get(key);
            if (alivcLivePlayer != null) {
                alivcLivePlayer.startPlay(url);
            }
        } else {
            UrlSource urlSource = new UrlSource();
            urlSource.setUri(url);
            mAliPlayer.setDataSource(urlSource);
            mAliPlayer.prepare();
        }
    }

    public void startPull(String url) {
        this.mPullUrl = url;
        if (url.startsWith(ARTC)) {
            mAlivcLivePlayer.startPlay(url);
        } else {
            UrlSource urlSource = new UrlSource();
            urlSource.setUri(url);
            mAliPlayer.setDataSource(urlSource);
            mAliPlayer.prepare();
        }
    }

    public void stopPull() {
        mAlivcLivePlayer.stopPlay();
        mHasPulled = false;
    }

    public void stopPull(String key) {
        AlivcLivePlayer alivcLivePlayer = mAlivcLivePlayerMap.get(key);
        if (alivcLivePlayer != null) {
            alivcLivePlayer.stopPlay();
        }
        mAlivcLivePlayerMap.remove(key);
    }

    public void stopCDNPull() {
        mAliPlayer.stop();
    }

    public boolean isPulling(String key) {
        MultiAlivcLivePlayer multiAlivcLivePlayer = mAlivcLivePlayerMap.get(key);
        if (multiAlivcLivePlayer != null) {
            return multiAlivcLivePlayer.isPulling();
        }
        return false;
    }

    public void linkMic(FrameLayout frameLayout, String pullUrl) {
        frameLayout.setVisibility(View.VISIBLE);
        setPullView(frameLayout, false);
        startPull(pullUrl);
        // setLiveMixTranscodingConfig(mAnchorId, viewerId);
    }

    public void clearLiveMixTranscodingConfig() {
        mALivcLivePusher.setLiveMixTranscodingConfig(null);
    }

    public void removeAudienceLiveMixTranscodingConfig(String audience, String anchorId) {
        if (TextUtils.isEmpty(audience)) {
            return;
        }
        for (AlivcLiveMixStream alivcLiveMixStream : mMultiInteractLiveMixStreamsArray) {
            if (audience.equals(alivcLiveMixStream.getUserId())) {
                mMultiInteractLiveMixStreamsArray.remove(alivcLiveMixStream);
                break;
            }
        }
        for (int index = 0; index < mMultiInteractLiveMixStreamsArray.size(); index++) {
            AlivcLiveMixStream alivcLiveMixStreamMixStream = mMultiInteractLiveMixStreamsArray.get(index);
            if (alivcLiveMixStreamMixStream.getZOrder() == 2) {//主播不需要重新排
                alivcLiveMixStreamMixStream.setUserId(mMultiInteractLiveMixStreamsArray.get(index).getUserId());
                alivcLiveMixStreamMixStream.setX(9);
                alivcLiveMixStreamMixStream.setY(9 + (index) * mAudiencelayoutArgs);
                alivcLiveMixStreamMixStream.setWidth(mAudiencelayoutArgs);
                alivcLiveMixStreamMixStream.setHeight(mAudiencelayoutArgs);
                alivcLiveMixStreamMixStream.setZOrder(2);
            }
        }

        //Array 中只剩主播 id，说明无人连麦
        if (mMultiInteractLiveMixStreamsArray.size() == 1 && mMultiInteractLiveMixStreamsArray.get(0).getUserId().equals(anchorId)) {
            if (mALivcLivePusher != null) {
                mALivcLivePusher.setLiveMixTranscodingConfig(null);
            }
        } else {
            mMixInteractLiveTranscodingConfig.setMixStreams(mMultiInteractLiveMixStreamsArray);
            if (mALivcLivePusher != null) {
                mALivcLivePusher.setLiveMixTranscodingConfig(mMixInteractLiveTranscodingConfig);
            }
        }
    }

    public void release() {
        try {
            stopCDNPull();
            stopPull();
            stopPublish();
            mALivcLivePusher.destroy();
            mAliPlayer.release();
            mAlivcLivePlayer.destroy();

            for (AlivcLivePlayer alivcLivePlayer : mAlivcLivePlayerMap.values()) {
                alivcLivePlayer.destroy();
            }
            mAlivcLivePlayerMap.clear();

            mMultiInteractLiveMixStreamsArray.clear();
            mMixInteractLiveTranscodingConfig.setMixStreams(null);
            if (mAlivcLivePlayer != null) {
                mALivcLivePusher.setLiveMixTranscodingConfig(mMixInteractLiveTranscodingConfig);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void addAudienceMixTranscodingConfig(String audience, FrameLayout frameLayout) {
        if (TextUtils.isEmpty(audience)) {
            return;
        }

        // 去重处理
        for (AlivcLiveMixStream stream : mMultiInteractLiveMixStreamsArray) {
            if (TextUtils.equals(stream.getUserId(), audience)) {
                // 已经有, 不再处理
                return;
            }
        }

        AlivcLiveMixStream audienceMixStream = new AlivcLiveMixStream();
        audienceMixStream.setUserId(audience);
        audienceMixStream.setX((int) frameLayout.getX());
        if (mMultiInteractLiveMixStreamsArray != null && mMultiInteractLiveMixStreamsArray.size() > 0) {
            audienceMixStream.setY((int) frameLayout.getY() + mMultiInteractLiveMixStreamsArray.size() * mAudiencelayoutArgs);
        }
        audienceMixStream.setWidth(mAudiencelayoutArgs);
        audienceMixStream.setHeight(mAudiencelayoutArgs);
        audienceMixStream.setZOrder(2);
        Log.d(TAG, "AlivcRTC audienceMixStream --- " + audienceMixStream.getUserId() + ", " + audienceMixStream.getWidth() + ", " + audienceMixStream.getHeight()
                + ", " + audienceMixStream.getX() + ", " + audienceMixStream.getY() + ", " + audienceMixStream.getZOrder());
        mMultiInteractLiveMixStreamsArray.add(audienceMixStream);
        mMixInteractLiveTranscodingConfig.setMixStreams(mMultiInteractLiveMixStreamsArray);
        if (mALivcLivePusher != null) {
            mALivcLivePusher.setLiveMixTranscodingConfig(mMixInteractLiveTranscodingConfig);
        }
    }

    public void updateMixItems(List<MixItem> mixItems) {
        if (mALivcLivePusher == null || CollectionUtil.isEmpty(mixItems)) {
            return;
        }

        ArrayList<AlivcLiveMixStream> mixStreams = new ArrayList<>();

        // 主播
        if (mixItems.size() == 1) {
            AlivcLiveMixStream anchorMixStream = CollectionUtil.getFirst(mMultiInteractLiveMixStreamsArray);
            if (anchorMixStream != null) {
                mixStreams.add(anchorMixStream);
            }
        } else {
            int canvasWidth = mAlivcLivePushConfig.getWidth();
            int canvasHeight = mAlivcLivePushConfig.getHeight();
            float canvasRatio = canvasWidth * 1f / canvasHeight;

            int screenWidth = AppUtil.getScreenWidth();
            int screenHeight = AppUtil.getScreenHeight();
            float screenRatio = screenWidth * 1f / screenHeight;

            boolean isHeightGrid = screenRatio < canvasRatio;

            Logger.i(TAG, String.format("canvas.size=(%s, %s)", canvasWidth, canvasHeight));
            Logger.i(TAG, String.format("canvasRatio=%s", canvasRatio));
            Logger.i(TAG, String.format("grid.size=(%s, %s)", screenWidth, screenHeight));
            Logger.i(TAG, String.format("screenRatio=%s", screenRatio));
            Logger.i(TAG, String.format("isHeightGrid=%s", isHeightGrid));

            for (int i = 0; i < mixItems.size(); i++) {
                MixItem mixItem = mixItems.get(i);
                int[] location = new int[2];
                View renderContainer = mixItem.renderContainer;
                if (renderContainer == null) {
                    continue;
                }

                renderContainer.getLocationOnScreen(location);
                int xInGrid = location[0];
                int yInGrid = location[1];

                AlivcLiveMixStream mixStream = new AlivcLiveMixStream();
                mixStream.setUserId(mixItem.userId);

                float scaleRatio = isHeightGrid
                        ? (canvasHeight * 1f / screenHeight)
                        : (canvasWidth * 1f / screenWidth);

                int finalWidth = (int) (renderContainer.getWidth() * scaleRatio);
                int finalHeight = (int) (renderContainer.getHeight() * scaleRatio);

                int scaledScreenWidth = (int) (screenWidth * scaleRatio);
                int scaledScreenHeight = (int) (screenHeight * scaleRatio);
                mixStream.setX((int) (xInGrid * scaleRatio) + (canvasWidth - scaledScreenWidth) / 2);
                mixStream.setY((int) (yInGrid * scaleRatio) + (canvasHeight - scaledScreenHeight) / 2);
                mixStream.setWidth(finalWidth);
                mixStream.setHeight(finalHeight);

                mixStream.setZOrder(mixItem.isAnchor ? 1 : 2);

                mixStreams.add(mixStream);

                Logger.i(TAG, String.format(Locale.getDefault(),
                        "\t%02d: xInGrid=%s, yInGrid=%s", i, xInGrid, yInGrid));
                Logger.i(TAG, String.format(Locale.getDefault(),
                        "\t%02d: %s", i, JSON.toJSONString(mixStream)));
            }
        }

        mMixInteractLiveTranscodingConfig.setMixStreams(mixStreams);
        mALivcLivePusher.setLiveMixTranscodingConfig(mMixInteractLiveTranscodingConfig);
    }

    /**
     * 创建 AlivcLivePlayer，用于多人连麦互动
     *
     * @param audiencePull 区分 AlivcLivePlayer 的 key
     */
    public boolean createAlivcLivePlayer(String audiencePull) {
        if (mAlivcLivePlayerMap.containsKey(audiencePull)) {
            return false;
        }
        MultiAlivcLivePlayer alivcLivePlayer = new MultiAlivcLivePlayer(mContext, AlivcLiveMode.AlivcLiveInteractiveMode);
        alivcLivePlayer.setAudienceId(audiencePull);
        alivcLivePlayer.setMultiInteractPlayInfoListener(new MultiInteractLivePushPullListener() {
            @Override
            public void onPullSuccess(String audiencePull) {
                if (mMultiInteractLivePushPullListener != null) {
                    mMultiInteractLivePushPullListener.onPullSuccess(audiencePull);
                }
            }

            @Override
            public void onPullError(String audiencePull, AlivcLivePlayError errorType, String errorMsg) {
                if (mMultiInteractLivePushPullListener != null) {
                    mMultiInteractLivePushPullListener.onPullError(audiencePull, errorType, errorMsg);
                }
            }

            @Override
            public void onPullStop(String audiencePull) {
                if (mMultiInteractLivePushPullListener != null) {
                    mMultiInteractLivePushPullListener.onPullStop(audiencePull);
                }
            }

            @Override
            public void onPushSuccess(String audiencePull) {
                if (mMultiInteractLivePushPullListener != null) {
                    mMultiInteractLivePushPullListener.onPushSuccess(audiencePull);
                }
            }

            @Override
            public void onPushError(String audiencePull) {
                if (mMultiInteractLivePushPullListener != null) {
                    mMultiInteractLivePushPullListener.onPushError(audiencePull);
                }
            }
        });
        mAlivcLivePlayerMap.put(audiencePull, alivcLivePlayer);
        return true;
    }

    private void initBeautyManager(long glContext) {
        if (mBeautyManager == null) {
            Log.d(TAG, "initBeautyManager start");
            if (mAlivcLivePushConfig.getLivePushMode() == AlivcLiveMode.AlivcLiveInteractiveMode) {
                //互动模式下，美颜初始化
                mBeautyManager = BeautyFactory.createBeauty(BeautySDKType.INTERACT_QUEEN, mContext);
            }
            // initialize in texture thread.
            mBeautyManager.init(glContext);
            mBeautyManager.setBeautyEnable(isBeautyEnable);
            mBeautyManager.switchCameraId(mCameraId);
            Log.d(TAG, "initBeautyManager end");
        }
    }

    private void destroyBeautyManager() {
        if (mBeautyManager != null) {
            mBeautyManager.release();
            mBeautyManager = null;
        }
    }

    public static class MixItem {
        public String userId;
        public boolean isAnchor;
        public View renderContainer;
    }
}
