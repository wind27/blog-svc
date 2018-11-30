package com.wind.blog.svc.model.rabbitmq;

import java.io.Serializable;

/**
 * Msg 消息结构
 *
 * @author qianchun 2018/8/28
 **/
public class Msg implements Serializable {

    /**
     * 消息类型
     */
    private String msgType;

    /**
     * 消息数据
     */
    private String body;

    /**
     * 队列名
     */
    private String queueName;

    /**
     * 消息发送者
     */
    private String sender;

    /**
     * 消费接受者
     */
    private String receiver;

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
}
