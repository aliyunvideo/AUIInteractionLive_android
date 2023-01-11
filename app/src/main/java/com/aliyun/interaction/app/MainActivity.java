package com.aliyun.interaction.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aliyun.aliinteraction.liveroom.activity.CreateRoomActivity;

/**
 * @author puke
 * @version 2022/11/24
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(new Intent(this, CreateRoomActivity.class));
    }
}
