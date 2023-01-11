package com.aliyun.aliinteraction.liveroom.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author puke
 * @version 2022/11/21
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetMeetingInfoRequest {

    public String id;
}
