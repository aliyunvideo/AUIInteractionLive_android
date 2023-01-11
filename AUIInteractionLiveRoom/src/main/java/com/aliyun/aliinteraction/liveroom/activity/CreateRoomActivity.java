package com.aliyun.aliinteraction.liveroom.activity;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.alivc.live.pusher.AlivcLivePushInstance;
import com.aliyun.aliinteraction.common.base.base.IBackPress;
import com.aliyun.aliinteraction.common.base.util.Utils;
import com.aliyun.aliinteraction.liveroom.R;
import com.aliyun.aliinteraction.liveroom.activity.base.AppBaseActivity;
import com.aliyun.aliinteraction.liveroom.fragment.LiveLoginFragment;
import com.aliyun.aliinteraction.liveroom.util.AppUtil;

/**
 * @author puke
 * @version 2021/5/13
 */
public class CreateRoomActivity extends AppBaseActivity {
    private boolean backTwiceToExit = false;
    private Fragment fragment;
    static {
        AlivcLivePushInstance.loadLibrariesOnce();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        updateFragmentViaRoomType();

    }

    private void updateFragmentViaRoomType() {
        fragment = new LiveLoginFragment();
        Utils.replaceWithFragment(this, R.id.toAddContainer, fragment);
    }

    @Override
    public void onBackPressed() {
        if (fragment instanceof IBackPress && ((IBackPress) fragment).interceptBackPress()) {
            return;
        }
        if (backTwiceToExit) {
            super.onBackPressed();
            postDelay(new Runnable() {
                @Override
                public void run() {
                    AppUtil.exitApplication();
                }
            }, 400);
            return;
        }

        this.backTwiceToExit = true;
        showToast("再按一次退出应用");
        postDelay(new Runnable() {
            @Override
            public void run() {
                backTwiceToExit = false;
            }
        }, 2000);
    }

    public void performLogin() {
        findViewById(R.id.loginButton).performClick();
    }
}
