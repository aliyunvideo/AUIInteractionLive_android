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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alivc.component.custom.AlivcLivePushCustomFilter;
import com.alivc.live.annotations.AlivcLiveMode;
import com.alivc.live.pusher.AlivcEncodeModeEnum;
import com.alivc.live.pusher.AlivcLiveBase;
import com.alivc.live.pusher.AlivcLiveBaseListener;
import com.alivc.live.pusher.AlivcLivePushConfig;
import com.alivc.live.pusher.AlivcLivePushConstants;
import com.alivc.live.pusher.AlivcLivePushError;
import com.alivc.live.pusher.AlivcLivePushErrorListener;
import com.alivc.live.pusher.AlivcLivePushInfoListener;
import com.alivc.live.pusher.AlivcLivePushLogLevel;
import com.alivc.live.pusher.AlivcLivePushNetworkListener;
import com.alivc.live.pusher.AlivcLivePushStats;
import com.alivc.live.pusher.AlivcLivePushStatsInfo;
import com.alivc.live.pusher.AlivcLivePusher;
import com.alivc.live.pusher.AlivcPreviewDisplayMode;
import com.alivc.live.pusher.AlivcQualityModeEnum;
import com.aliyun.aliinteraction.beauty.BeautyFactory;
import com.aliyun.aliinteraction.beauty.BeautyInterface;
import com.aliyun.aliinteraction.beauty.constant.BeautySDKType;
import com.aliyun.aliinteraction.common.base.log.Logger;
import com.aliyun.aliinteraction.core.utils.AssetUtil;
import com.aliyun.aliinteraction.liveroom.live.exposable.AliLiveMediaStreamOptions;
import com.aliyun.aliinteraction.liveroom.BuildConfig;
import com.aliyun.aliinteraction.player.exposable.CanvasScale;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
/**
 * ??????????????????
 */
public class LivePushManager implements AlivcLiveBaseListener {

    private static final String TAG = LivePushManager.class.getSimpleName();
    private static final String LIVE_PUSH_CONFIG_EXTRA_INFO = "channel_aliyun_solution";

    private AlivcLivePusher mALivcLivePusher;
    private AliLivePusherOptions mAliLivePusherOptions;
    private SurfaceView mSurfaceView;

    private boolean isStartPush = false;
    private boolean isPause = false;

    // ????????????????????????
    private String mCurrentPublishUrl;
    private final Context mContext;

    private Callback callback;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private SurfaceHolder.Callback surfaceViewCallback;
    private  AlivcLivePushConfig alivcLivePushConfig;
    private BeautyInterface mBeautyManager;
    private boolean isBeautyEnable = true;

    public LivePushManager(Context context, AliLivePusherOptions aliLivePusherOptions) {
        mContext = context;
        mAliLivePusherOptions = aliLivePusherOptions;
        init();
    }

    private void init() {
        if (mALivcLivePusher == null) {
            mALivcLivePusher = new AlivcLivePusher();
        }

        if (mAliLivePusherOptions == null) {
            alivcLivePushConfig = new AlivcLivePushConfig();
            alivcLivePushConfig.setPreviewDisplayMode(AlivcPreviewDisplayMode.ALIVC_LIVE_PUSHER_PREVIEW_ASPECT_FILL);
        } else {
            alivcLivePushConfig = convertAlivcLivePushConfig(mAliLivePusherOptions);
        }

        alivcLivePushConfig.setPausePushImage(getPushPauseImagePath());

        // ????????????
        if (BuildConfig.DEBUG) {
            AlivcLiveBase.setLogLevel(AlivcLivePushLogLevel.AlivcLivePushLogLevelError);
            AlivcLiveBase.setConsoleEnabled(true);
            String logPath = getFilePath(mContext, "log_path");
            // full log file limited was kLogMaxFileSizeInKB * 5 (parts)
            int maxPartFileSizeInKB = 100 * 1024 * 1024; //100G
            AlivcLiveBase.setLogDirPath(logPath, maxPartFileSizeInKB);
        } else {
            AlivcLiveBase.setLogLevel(AlivcLivePushLogLevel.AlivcLivePushLogLevelNone);
            AlivcLiveBase.setConsoleEnabled(false);
        }
        // ??????sdk
        AlivcLiveBase.setListener(this);
        AlivcLiveBase.registerSDK();

        mALivcLivePusher.init(mContext, alivcLivePushConfig);
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
                if (alivcLivePushConfig != null && alivcLivePushConfig.getLivePushMode() == AlivcLiveMode.AlivcLiveInteractiveMode) {
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
        // ?????????????????? AUIInteractionLiveRoom/src/main/assets/background_push.png ??????
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
        //??????SD???????????????
        if (MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ) {
            logFilePath = context.getExternalFilesDir(dir).getAbsolutePath() ;
        }else{
            //??????????????????????????????
            logFilePath = context.getFilesDir() + File.separator + dir;
        }
        File file = new File(logFilePath);
        if(!file.exists()){//??????????????????????????????
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

    //??????????????????AlivcLivePushConfig??????
    private AlivcLivePushConfig convertAlivcLivePushConfig(AliLivePusherOptions options) {
        AlivcLivePushConfig alivcLivePushConfig = new AlivcLivePushConfig();
        alivcLivePushConfig.setExtraInfo(LIVE_PUSH_CONFIG_EXTRA_INFO);

        AliLiveMediaStreamOptions mediaStreamOptions = options.mediaStreamOptions;
        if (mediaStreamOptions != null) {
            alivcLivePushConfig.setQualityMode(AlivcQualityModeEnum.QM_CUSTOM);
            alivcLivePushConfig.setEnableBitrateControl(true);
            alivcLivePushConfig.setVideoOnly(mediaStreamOptions.isVideoOnly);
            alivcLivePushConfig.setAudioOnly(mediaStreamOptions.isAudioOnly);
            alivcLivePushConfig.setTargetVideoBitrate(mediaStreamOptions.videoBitrate);
            alivcLivePushConfig.setFps(mediaStreamOptions.fps);
            alivcLivePushConfig.setResolution(mediaStreamOptions.getResolution());
            alivcLivePushConfig.setCameraType(mediaStreamOptions.getCameraType());
            alivcLivePushConfig.setVideoEncodeMode(mediaStreamOptions.getEncodeMode());
            alivcLivePushConfig.setVideoEncodeGop(mediaStreamOptions.getEncodeGop());
            alivcLivePushConfig.setPreviewOrientation(mediaStreamOptions.getPreviewOrientation());
            alivcLivePushConfig.setPreviewDisplayMode(mediaStreamOptions.getPreviewDisplayMode());
            switch (mediaStreamOptions.getCameraType()) {
                case CAMERA_TYPE_BACK:
                    mCameraId = CAMERA_TYPE_BACK.getCameraId();
                    break;
                case CAMERA_TYPE_FRONT:
                    mCameraId = CAMERA_TYPE_FRONT.getCameraId();
                    break;
            }
        }
        alivcLivePushConfig.setAudioEncodeMode(AlivcEncodeModeEnum.Encode_MODE_HARD);
        alivcLivePushConfig.setVideoEncodeMode(AlivcEncodeModeEnum.Encode_MODE_HARD);

        return alivcLivePushConfig;
    }

    /**
     * ???????????????
     *
     * @param open ????????????
     */
    public void setFlash(boolean open) {
        if (mALivcLivePusher != null && isPreviewedOrPushed()) {
            mALivcLivePusher.setFlash(open);
        }
    }

    /**
     * @return ???????????????????????????
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

        if (mSurfaceView == null) {
            Logger.w(TAG, "resumePublish mSurfaceView is null");
            return;
        }

        // ????????????, ???????????????
        if (currentStatus != AlivcLivePushStats.PREVIEWED) {
            if (currentStatus == AlivcLivePushStats.INIT) {
                Logger.i(TAG, "resumePublish start preview");
                doStartPreviewByMediaSdk(mSurfaceView);
            } else {
                Logger.w(TAG, "resumePublish currentStatus is error");
                return;
            }
        }

        // ????????????
        Logger.i(TAG, "resumePublish start publish");
        startPublish(mCurrentPublishUrl);
    }

    /**
     * ????????????
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
        mALivcLivePusher.startPush(url);
        isStartPush = true;
        return mSurfaceView;
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    public String getCurrentPublishUrl() {
        return mCurrentPublishUrl;
    }

    /**
     * ????????????
     */
    public void stopPublish() {
        stopPublish(true);
    }

    /**
     * ????????????
     */
    private void stopPublish(boolean reportEvent) {
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
                    // ????????????????????????????????????
                    Logger.i(TAG, "surfaceCreated, pusher is paused");
                    return;
                }

                if (currentStatus != AlivcLivePushStats.PREVIEWED) {
                    doStartPreviewByMediaSdk(surfaceView);
                }

                resumeLive();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                lastStatus = mALivcLivePusher == null ? null : mALivcLivePusher.getCurrentStatus();
                Logger.i(TAG, String.format("surfaceDestroyed, lastStatus is %s", lastStatus));
                pauseLive(false);
            }
        };
        surfaceView.getHolder().addCallback(surfaceViewCallback);
        return surfaceView;
    }

    private void doStartPreviewByMediaSdk(SurfaceView surfaceView) {
        if (mALivcLivePusher != null) {
            try {
                mALivcLivePusher.startPreview(surfaceView);
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
     * ????????????
     */
    public View startPreview() {
        if (mALivcLivePusher == null) {
            init();
        }

        if (mSurfaceView == null) {
            mSurfaceView = createSurfaceView();
        } else {
            doStartPreviewByMediaSdk(mSurfaceView);
        }

        return mSurfaceView;
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
     * ????????????View
     *
     * @return
     */
    public SurfaceView getAliLiveRenderView() {
        return mSurfaceView;
    }

    /**
     * ??????????????????
     */
    public void destroy() {
        stopPublish();

        if (mALivcLivePusher != null) {
            try {
                mALivcLivePusher.destroy();
            } catch (Throwable e) {
                Logger.e(TAG, "destroy error", e);
            }
            mALivcLivePusher = null;
        }
    }

    /**
     * ????????????
     */
    public void setMute(boolean mute) {
        mALivcLivePusher.setMute(mute);
    }

    /**
     * ????????????
     */
    public void pauseLive() {
        pauseLive(true);
    }

    /**
     * ????????????
     */
    private void pauseLive(boolean setPauseState) {
        if (mALivcLivePusher == null) {
            return;
        }

        AlivcLivePushStats pushStats = mALivcLivePusher.getCurrentStatus();
        if (pushStats != AlivcLivePushStats.PUSHED &&
                pushStats != AlivcLivePushStats.PAUSED &&
                pushStats != AlivcLivePushStats.PREVIEWED) {
            return;
        }
        try {
            mALivcLivePusher.pause();
            if (setPauseState) {
                isPause = true;
            }
        } catch (Throwable e) {
            Logger.e(TAG, "pauseLive: error:", e);
        }
    }

    /**
     * ????????????
     */
    public void resumeLive() {
        if (mALivcLivePusher == null) {
            return;
        }

        AlivcLivePushStats pushStats = mALivcLivePusher.getCurrentStatus();
        if (pushStats != AlivcLivePushStats.PAUSED &&
                pushStats != AlivcLivePushStats.ERROR) {
            return;
        }
        try {
            mALivcLivePusher.resume();
            isPause = false;
        } catch (Throwable e) {
            Logger.e(TAG, "resumeLive: error", e);
        }
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
     * ???????????????
     */
    public void switchCamera() {
        if (mALivcLivePusher == null || isPushDisable()) {
            return;
        }
        if (mCameraId == CAMERA_TYPE_FRONT.getCameraId()) {
            mCameraId = CAMERA_TYPE_BACK.getCameraId();
        } else {
            mCameraId = CAMERA_TYPE_FRONT.getCameraId();
        }

        mALivcLivePusher.switchCamera();

        if (mBeautyManager != null) {
            mBeautyManager.switchCameraId(mCameraId);
        }
    }

    /**
     * ??????????????????
     *
     * @param mirror
     */
    public void setPreviewMirror(boolean mirror) {
        if (mALivcLivePusher == null) {
            return;
        }
        if (isPushDisable()) {
            return;
        }
        mALivcLivePusher.setPreviewMirror(mirror);
    }

    /**
     * ??????????????????
     *
     * @param mirror
     */
    public void setPushMirror(boolean mirror) {
        if (mALivcLivePusher == null) {
            return;
        }
        if (isPushDisable()) {
            return;
        }
        mALivcLivePusher.setPushMirror(mirror);
    }

    private boolean isPushDisable() {
        AlivcLivePushStats pushStats = mALivcLivePusher.getCurrentStatus();
        return pushStats != AlivcLivePushStats.PREVIEWED && pushStats != AlivcLivePushStats.PUSHED;
    }
    /**
     * ????????????
     * ????????????????????????????????????Error??????????????????????????????????????????, ???Error?????????????????????????????????(??????reconnectPushAsync??????)????????????destory???????????????
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
     * ??????????????????????????????AlivcLivePusherNetworkDelegate?????????Error?????????????????????????????????,
     * ???Error?????????????????????????????????(??????restartPush????????????)????????????destory?????????????????????????????????????????????????????????RTMP
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
            //???????????????
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

    private void initBeautyManager(long glContext) {
        if (mBeautyManager == null) {
            Log.d(TAG, "initBeautyManager start");
            if (alivcLivePushConfig.getLivePushMode() == AlivcLiveMode.AlivcLiveBasicMode) {
                //????????????????????????????????????
                mBeautyManager = BeautyFactory.createBeauty(BeautySDKType.QUEEN, mContext);
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

}
