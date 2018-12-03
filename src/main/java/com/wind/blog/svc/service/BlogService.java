package com.wind.blog.svc.service;

import com.wind.blog.model.Blog;
import com.wind.blog.svc.mapper.BlogMapperEx;
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
public class BlogService {
    private final static Logger logger = LoggerFactory.getLogger(BlogService.class);

    @Autowired
    private BlogMapperEx blogMapperEx;

    public Blog findById(Long id) {
        return blogMapperEx.findById(id);
    }

    public boolean add(Blog blog) {
        return blogMapperEx.insert(blog) > 0;
    }
}

