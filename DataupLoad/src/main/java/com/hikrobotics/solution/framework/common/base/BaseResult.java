/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  cn.hutool.extra.spring.SpringUtil
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.node.ObjectNode
 *  com.hikrobotics.solution.framework.common.locale.LocaleUtil
 *  com.hikrobotics.solution.framework.common.log.MySlf4j
 */
package com.hikrobotics.solution.framework.common.base;

import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hikrobotics.solution.framework.common.locale.LocaleUtil;
import com.hikrobotics.solution.framework.common.log.MySlf4j;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseResult
implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(BaseResult.class);
    private static final long serialVersionUID = -1947235086372745273L;
    private Boolean success;
    private Object data;
    private Integer code;
    private String message;

    public BaseResult() {
        this.code = Integer.valueOf(0);
    }

    public static BaseResult build() {
        return new BaseResult().ok();
    }

    public BaseResult ok() {
        return this.setSuccess(Boolean.valueOf(true));
    }

    public BaseResult error() {
        return this.setSuccess(Boolean.valueOf(false));
    }

    public BaseResult data(Object data) {
        return this.setData(data);
    }

    public BaseResult data(Object data, String key) {
        return this.setData(data, key);
    }

    public BaseResult msg(String msg) {
        this.message = LocaleUtil.getMsg(msg);
        return this;
    }

    public BaseResult msgBody(String msg) {
        this.message = msg;
        return this;
    }

    public BaseResult error(String code) {
        this.code = Integer.valueOf(code);
        this.message = LocaleUtil.getMsg(code);
        this.success = Boolean.valueOf(false);
        return this;
    }

    public BaseResult error(String code, Object ... params) {
        this.code = Integer.valueOf(code);
        this.message = LocaleUtil.getMsg(code, params);
        this.success = Boolean.valueOf(false);
        return this;
    }

    public BaseResult code(String code) {
        this.code = Integer.valueOf(code);
        return this;
    }

    public BaseResult code(Integer code) {
        this.code = code;
        return this;
    }

    public BaseResult log(String msg) {
        return this.log(msg, null);
    }

    public BaseResult log(String msg, String param) {
        String code2 = String.valueOf(this.code);
        MySlf4j.error(2, "{0} [param={1}][code={2}]", msg, param, code2);
        return this;
    }

    public BaseResult log(String msg, Object param) {
        String code2 = String.valueOf(this.code);
        MySlf4j.error(2, "{0} [param={1}][code={2}]", msg, param == null ? null : param.toString(), code2);
        return this;
    }

    public String toJsonString() {
        try {
            ObjectMapper objectMapper = (ObjectMapper)SpringUtil.getBean(ObjectMapper.class);
            return objectMapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkOk() {
        if (this.success != null) {
            return this.success;
        }
        return this.code == 0;
    }

    private BaseResult setData(Object data) {
        this.data = data;
        return this;
    }

    private BaseResult setData(Object data, String key) {
        ObjectMapper objectMapper = (ObjectMapper)SpringUtil.getBean(ObjectMapper.class);
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.putPOJO(key, data);
        this.data = objectNode;
        return this;
    }

    private BaseResult setCode(Integer code) {
        this.code = code;
        return this;
    }

    private BaseResult setSuccess(Boolean success) {
        this.success = success;
        return this;
    }

    private BaseResult setMessage(String message) {
        this.message = message;
        return this;
    }

    public Boolean getSuccess() {
        return this.success;
    }

    public Object getData() {
        return this.data;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }
}
