package com.wind.blog.svc.service;

import com.alibaba.fastjson.JSONObject;
import com.wind.blog.mapper.BlogMapper;
import com.wind.blog.mapper.LinkMapper;
import com.wind.blog.model.Blog;
import com.wind.blog.model.Link;
import com.wind.blog.svc.common.Constant;
import com.wind.blog.svc.common.RedisKey;
import com.wind.blog.svc.mapper.BlogMapperEx;
import com.wind.blog.svc.mapper.LinkMapperEx;
import com.wind.blog.svc.model.emun.BlogSource;
import com.wind.blog.svc.model.emun.MsgType;
import com.wind.blog.svc.model.emun.QueueName;
import com.wind.blog.svc.model.rabbitmq.Msg;
import com.wind.blog.svc.service.base.RabbitmqService;
import com.wind.blog.svc.service.base.RedisService;
import com.wind.blog.svc.source.aliyun.AliyunParser;
import com.wind.blog.svc.source.csdn.CSDNParser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LinkParseService {

    private final static Logger logger = LoggerFactory.getLogger(LinkParseService.class);

    @Autowired
    private RabbitmqService rabbitmqService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private LinkMapperEx linkMapperEx;

    @Autowired
    private BlogMapperEx blogMapperEx;

    private static List<BlogSource> blogSourceList = new ArrayList<>();

    static {
        blogSourceList.add(BlogSource.ALIYUN);
        blogSourceList.add(BlogSource.CSDN);
    }

    /**
     * 开始解析该 blogSource 下, 按照 catalog(分类), 解析各个分类下的blog URL 并发送 rabbitmq
     *
     * @param blogSource blogSource
     */
    public void start(BlogSource blogSource) {
        List<String> catalogList;
        if (blogSource == BlogSource.ALIYUN) {
            catalogList = AliyunParser.catalogList;
        } else if (blogSource == BlogSource.CSDN) {
            catalogList = new ArrayList<>();
        } else {
            logger.error("[LINK任务] blogSource 不正确, 参数: blogSource={}", blogSource);
            return;
        }

        if (CollectionUtils.isEmpty(catalogList)) {
            logger.error("[LINK任务] catalog为空, blogSource={}", blogSource);
            return;
        }
        for (String catalog : catalogList) {
            if (StringUtils.isEmpty(catalog)) {
                continue;
            }
            parseByCatalog(catalog, blogSource);
        }
    }

    /**
     * 根据 catalog 解析该分类下的 BLOG 链接
     *
     * @param blogSource blogSource
     * @param catalog 分类
     */
    private void parseByCatalog(String catalog, BlogSource blogSource) {
        if (blogSource == null || !blogSourceList.contains(blogSource)) {
            logger.error("[LINK任务] blogSource 不正确, 参数: blogSource={}", blogSource);
            return;
        }
        if (blogSource == BlogSource.ALIYUN && !AliyunParser.catalogList.contains(catalog)) {
            logger.error("[LINK任务] catalog 不正确, 参数: catalog={}", catalog);
            return;
        } else if (blogSource == BlogSource.CSDN && !CSDNParser.catalogList.contains(catalog)) {
            logger.error("[LINK任务] catalog 不正确, 参数: catalog={}", catalog);
            return;
        }
        String url = null;
        int num = 0;
        while (true) {
            try {
                if (blogSource == BlogSource.ALIYUN) {
                    url = AliyunParser.getUrl(catalog, ++num);// 分页URL
                }
                boolean flag = this.parseAndSendMsg(url, blogSource, catalog);
                if(!flag) {
                    break;
                }
//                if(!flag || num>10) {
//                    break;
//                }
            } catch (Exception e) {
                logger.error("[LINK任务] link 解析异常, 参数: url={}", url);
            }
        }
        logger.info("[LINK任务] 解析完成, 参数: catalog={}", catalog);
    }

    /**
     * 解析BLOG的URL, 封装MSG, 发送rabbitmq
     * 
     * @param url 带解析的分页URL
     * @param blogSource BLOG来源
     */
    private boolean parseAndSendMsg(String url, BlogSource blogSource, String cataLog) {
        try {
            if (StringUtils.isEmpty(url)) {
                logger.error("[LINK解析] 参数错误, url={}", url);
                return false;
            }
            // 处理URL
            if (BlogSource.ALIYUN == blogSource) {
                url = this.dealUrl(Constant.ALIYUN_REGEX, url);
            } else if (BlogSource.CSDN == blogSource) {

            }
            List<String> blogUrlList = AliyunParser.getBlogURLFromPage(url);
            if (CollectionUtils.isEmpty(blogUrlList)) {
                return false;
            }
            for (String blogUrl : blogUrlList) {
                if (StringUtils.isEmpty(blogUrl)) {
                    continue;
                }
                //判断是否已经存在
                boolean exists = redisService.sHasKey(RedisKey.TASK_LINK_URL_LIST, blogUrl);
                if(exists) {
                    continue;
                }
                Link link = linkMapperEx.findByUrl(blogUrl);
                if(link != null) {
                    Blog blog = blogMapperEx.findById(link.getBlogId());
                    blog.setTags(cataLog);
                    blogMapperEx.updateByPrimaryKey(blog);
                    redisService.sSet(RedisKey.TASK_LINK_URL_LIST, blogUrl);
                    continue;
                }
                Thread.sleep(10);
                Map<String, Object> data = new HashMap<>();
                data.put("url", blogUrl);
                data.put("cataLog", cataLog);
                data.put("blogSource", blogSource.getValue());
                Msg msg = new Msg();
                msg.setMsgType(MsgType.BLOG_ADD);
                msg.setBody(JSONObject.toJSONString(data));
                msg.setQueueName(QueueName.QUEUE_BLOG_URL_PARSE);
                rabbitmqService.send(msg);
            }
            logger.info("[LINK解析] 完成, url={}", url);
            return true;
        } catch (Exception e) {
            logger.error("[LINK解析] 异常, url={}", url, e);
            return false;
        }
    }

    /**
     * 正则处理链接URL
     *
     * @param urlRegx 链接正则
     * @param url 链接URL
     * @return 返回结果
     */
    private String dealUrl(String urlRegx, String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        url = url.trim();
        Pattern pattern = Pattern.compile(urlRegx);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            url = url.substring(matcher.start(), matcher.end());
            return url;
        } else {
            return null;
        }
    }
}
