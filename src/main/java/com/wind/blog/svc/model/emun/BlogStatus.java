package com.wind.blog.svc.model.emun;

/**
 * blog 状态
 * @author qianchun
 */
public enum BlogStatus {

    EDIT("edit", 1), PUBLISH("publish", 2), DISABLE("delete", 3);

    private String type;

    private Integer value;

    BlogStatus(String type, Integer value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public Integer getValue() {
        return value;
    }
}
