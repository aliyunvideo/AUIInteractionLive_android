package com.aliyun.aliinteraction.beauty.constant;

import static com.aliyun.aliinteraction.beauty.constant.BeautyImageFormat.kDefault;
import static com.aliyun.aliinteraction.beauty.constant.BeautyImageFormat.kNV21;
import static com.aliyun.aliinteraction.beauty.constant.BeautyImageFormat.kRGB;
import static com.aliyun.aliinteraction.beauty.constant.BeautyImageFormat.kRGBA;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({kDefault, kRGB, kNV21, kRGBA})
public @interface BeautyImageFormat {
    int kDefault = -1;
    int kRGB = 0;
    int kNV21 = 1;
    int kRGBA = 2;
}
