package com.hikrobotics.solution.module.alarm.dto;

public class PlaySoundWsMsgDTO {
    private String uri;
    private Integer playCount;

    public String getUri() { return uri; }
    public PlaySoundWsMsgDTO setUri(String uri) { this.uri = uri; return this; }

    public Integer getPlayCount() { return playCount; }
    public PlaySoundWsMsgDTO setPlayCount(Integer playCount) { this.playCount = playCount; return this; }
}
