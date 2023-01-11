package com.aliyun.aliinteraction.liveroom.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LiveCdnPullInfo implements Serializable {

    @JsonProperty("flv_url")
    public String flvUrl;

    @JsonProperty("hls_url")
    public String hlsUrl;

    @JsonProperty("rtmp_url")
    public String rtmpUrl;

    @JsonProperty("rts_url")
    public String rtsUrl;

}
