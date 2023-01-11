package com.aliyun.aliinteraction.liveroom.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author puke
 * @version 2022/11/21
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingInfo {

    public List<LinkMicItemModel> members;
}
