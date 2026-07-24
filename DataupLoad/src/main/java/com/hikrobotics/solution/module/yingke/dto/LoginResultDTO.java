package com.hikrobotics.solution.module.yingke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResultDTO {
    @JsonProperty("UserId")
    private String UserId;
    @JsonProperty("EmployeeId")
    private String EmployeeId;
    @JsonProperty("UserCode")
    private String UserCode;
    @JsonProperty("UserName")
    private String UserName;
    @JsonProperty("InvOrg")
    private Integer InvOrg;

    public String getUserId() { return UserId; }
    @JsonProperty("UserId") public void setUserId(String UserId) { this.UserId = UserId; }

    public String getEmployeeId() { return EmployeeId; }
    @JsonProperty("EmployeeId") public void setEmployeeId(String EmployeeId) { this.EmployeeId = EmployeeId; }

    public String getUserCode() { return UserCode; }
    @JsonProperty("UserCode") public void setUserCode(String UserCode) { this.UserCode = UserCode; }

    public String getUserName() { return UserName; }
    @JsonProperty("UserName") public void setUserName(String UserName) { this.UserName = UserName; }

    public Integer getInvOrg() { return InvOrg; }
    @JsonProperty("InvOrg") public void setInvOrg(Integer InvOrg) { this.InvOrg = InvOrg; }
}
