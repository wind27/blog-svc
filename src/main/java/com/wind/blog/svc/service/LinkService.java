package com.wind.blog.svc.service;

import com.wind.blog.model.Link;
import com.wind.blog.svc.mapper.LinkMapperEx;
import com.wind.blog.svc.service.base.RedisService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * BlogService
 *
 * @author qianchun 2018/9/3
 **/
@Service
public class LinkService {
    private final static Logger logger = LoggerFactory.getLogger(LinkService.class);

    @Autowired
    private RedisService redisService;

    @Autowired
    private LinkMapperEx linkMapperEx;

    public Link findByUrl(String url) {
        return linkMapperEx.findByUrl(url);
    }

    public boolean add(Link link) {
        return linkMapperEx.insert(link) > 0;
    }

    public boolean exists(String url) {
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        Object obj = redisService.get(url.trim());

        if (obj != null) {
            return true;
        }
        Link link = linkMapperEx.findByUrl(url);

        if (link != null && link.getBlogId() != null) {
            return true;
        }
        return false;
    }
}
