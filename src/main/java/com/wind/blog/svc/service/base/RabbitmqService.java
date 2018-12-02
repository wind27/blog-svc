package com.wind.blog.svc.service.base;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.wind.blog.model.Blog;
import com.wind.blog.model.Link;
import com.wind.blog.svc.common.Constant;
import com.wind.blog.svc.common.RedisKey;
import com.wind.blog.svc.config.RabbitMqConfig;
import com.wind.blog.svc.model.emun.BlogSource;
import com.wind.blog.svc.model.rabbitmq.Msg;
import com.wind.blog.svc.service.BlogParseService;
import com.wind.blog.svc.service.BlogService;
import com.wind.blog.svc.service.LinkService;
import com.wind.blog.svc.source.aliyun.AliyunParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * RedisService
 *
 * @author qianchun 2018/9/4
 **/
@Service
public class RabbitmqService {
    private final static Logger logger = LoggerFactory.getLogger(RabbitmqService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private BlogParseService blogParseService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private BlogService blogService;

    @Autowired
    public Queue userQueue;

    @Autowired
    private RedisService redisService;

    /**
     * 发送消息
     *
     * @param msg 消息
     */
    public void send(Msg msg) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_DIRECT_BLOGLINKPARSE,
                RabbitMqConfig.ROUTING_BLOGLINKPARSE, JSONObject.toJSONString(msg));
        logger.info("[RABBITMQ] send msg: {}", msg);
    }

    @Bean
    public SimpleMessageListenerContainer messageContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(userQueue);
        container.setExposeListenerChannel(true);
        container.setMaxConcurrentConsumers(1);
        container.setConcurrentConsumers(1);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL); // 设置确认模式手工确认
        container.setMessageListener(new ChannelAwareMessageListener() {
            @Override
            public void onMessage(Message message, Channel channel) throws Exception {
                try {
                    byte[] body = message.getBody();
                    if (body == null) {
                        return;
                    }

                    String content = new String(body);
                    logger.info("receive msg : " + content);
                    JSONObject json = JSONObject.parseObject(content);
                    Msg msg = JSONObject.toJavaObject(json, Msg.class);
                    if (msg == null) {
                        return;
                    }

                    JSONObject jsonObject = JSONObject.parseObject(msg.getBody());
                    String blogUrl = jsonObject.getString("url");
                    Integer blogSource = jsonObject.getInteger("blogSource");
                    if (StringUtils.isEmpty(blogUrl) || blogSource == null) {
                        return;
                    }

                    // 判断是否已经存在
                    boolean exists = redisService.sHasKey(RedisKey.TASK_LINK_URL_LIST, blogUrl);
                    if (exists) {
                        return;
                    }
                    redisService.sSet(RedisKey.TASK_LINK_URL_LIST, blogUrl);
                    Link link = linkService.findByUrl(blogUrl);
                    if (link != null) {
                        return;
                    }

                    Thread.sleep(20);
                    link = new Link();
                    link.setSource(BlogSource.ALIYUN.getValue());
                    link.setUrl(blogUrl);
                    link.setIsParse(Constant.LINK_IS_PARSE.YES);

                    Blog blog = null;
                    if (blogSource == BlogSource.ALIYUN.getValue()) {
                        blog = AliyunParser.getBlogFromUrl(blogUrl);
                    } else if (blogSource == BlogSource.CSDN.getValue()) {

                    }

                    if (blog == null || StringUtils.isEmpty(blog.getTitle()) || StringUtils.isEmpty(blog.getContent())
                            || blog.getTitle().length() > 255) {
                        return;
                    }
                    blogService.add(blog);
                    link.setBlogId(blog.getId());
                    boolean flag = linkService.add(link);
                    if (flag) {
                        redisService.sSet(RedisKey.TASK_LINK_URL_LIST, blogUrl);
                    }
                } catch (InterruptedException e) {
                    logger.error("消费失败, userQueue={}", userQueue, e);
                    throw e;
                } finally {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                }

            }
        });
        return container;
    }
}
