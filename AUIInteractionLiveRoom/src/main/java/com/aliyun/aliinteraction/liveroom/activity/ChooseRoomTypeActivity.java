package com.aliyun.aliinteraction.liveroom.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.aliyun.aliinteraction.common.base.callback.Callbacks;
import com.aliyun.aliinteraction.common.base.util.CommonUtil;
import com.aliyun.aliinteraction.common.roombase.Const;
import com.aliyun.aliinteraction.liveroom.LivePrototype;
import com.aliyun.aliinteraction.liveroom.R;
import com.aliyun.aliinteraction.liveroom.activity.base.AppBaseActivity;
import com.aliyun.aliinteraction.liveroom.live.exposable.AliLiveMediaStreamOptions;
import com.aliyun.aliinteraction.liveroom.sp.LastLiveSp;
import com.aliyun.aliinteraction.liveroom.util.AppUtil;
import com.aliyun.aliinteraction.uikit.uibase.util.ExStatusBarUtils;

public class ChooseRoomTypeActivity extends AppBaseActivity {

    private EditText liveroomTitleEdt;//直播间title

    private EditText liveroomTipsEdt;//直播间公告

    private Button backBtn;//返回

    private Button createRoomBtn;//创建直播间

    private RadioGroup liveTypeGroup;//直播类型

    private int typeLive = 0;//直播类型 0 基础直播 1 连麦直播

    private TextView noticeTips;

    private TextView titleTips;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExStatusBarUtils.setStatusBarColor(this, AppUtil.getColor(R.color.bus_login_status_bar_color));
        setContentView(R.layout.activity_choose_room);
        liveroomTitleEdt = findViewById(R.id.liveroom_title);
        liveroomTitleEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count==0) {
                    createRoomBtn.setBackgroundResource(R.drawable.ilr_bg_orange_button_radius_enable);
                    createRoomBtn.setEnabled(false);
                    titleTips.setVisibility(View.GONE);
                }else{
                    titleTips.setVisibility(View.VISIBLE);
                    createRoomBtn.setEnabled(true);
                    createRoomBtn.setBackgroundResource(R.drawable.ilr_bg_orange_button_radius_selector);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        liveroomTipsEdt = findViewById(R.id.liveroom_tips);
        liveroomTipsEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count==0) {
                    noticeTips.setVisibility(View.GONE);
                }else{
                    noticeTips.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        liveTypeGroup = findViewById(R.id.live_group);
        noticeTips = findViewById(R.id.liveroom_notice_tips);
        titleTips = findViewById(R.id.liveroom_title_tips);
        liveTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                if(checkedId==0){
                    typeLive = 0;
                }else{
                    typeLive = 1;
                }
            }
        });
        createRoomBtn = findViewById(R.id.create_room_btn);
        createRoomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoLivePage(null, Const.getUserId());
            }
        });
        backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /**
     * 跳转直播页面
     *
     * @param liveId   直播Id
     * @param anchorId 主播Id
     */
    private void gotoLivePage(final String liveId, final String anchorId) {
        if(TextUtils.isEmpty(liveroomTitleEdt.getText().toString())){
            CommonUtil.showToast(ChooseRoomTypeActivity.this,"直播标题不能为空");
            return;
        }
        final LivePrototype.OpenLiveParam param = new LivePrototype.OpenLiveParam();
        param.liveId = liveId;
        param.nick = Const.getUserId();
        param.title = liveroomTitleEdt.getText().toString();
        param.notice = liveroomTipsEdt.getText().toString();
        param.model = typeLive;
        final Callbacks.Lambda<String> callback = new Callbacks.Lambda<>(new Callbacks.Lambda.CallbackWrapper<String>() {
            @Override
            public void onCall(boolean success, String data, String errorMsg) {
                if (success) {
                    // 成功后, 记录该场直播, 便于下次快速进入
                    LastLiveSp lastLiveSp = LastLiveSp.INSTANCE;
                    lastLiveSp.setLastLiveId(liveId == null ? data : liveId);
                    lastLiveSp.setLastLiveAnchorId(anchorId);
                } else {
                    ChooseRoomTypeActivity.this.showToast(errorMsg);
                }
            }
        });

        boolean isAnchor = TextUtils.equals(Const.getUserId(), anchorId);
        if (isAnchor) {
            // 主播端: 开启直播
            param.role = LivePrototype.Role.ANCHOR;

            // 推流支持动态配置
            final AliLiveMediaStreamOptions pusherOptions = new AliLiveMediaStreamOptions();
            param.mediaPusherOptions = pusherOptions;
            param.title = liveroomTitleEdt.getText().toString();
        } else {
            // 观众端: 观看直播
            param.role = LivePrototype.Role.AUDIENCE;
        }
        if (isAnchor) {
            LivePrototype.getInstance().setup(context, param, callback);
        } else {
            LivePrototype.getInstance().setup(context, param, callback);
        }
        finish();
    }
}
