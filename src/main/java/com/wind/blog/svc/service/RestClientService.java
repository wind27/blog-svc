package com.wind.blog.svc.service;

import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RestClientService
 *
 * @author qianchun 2018/9/29
 **/
public class RestClientService<T> {

    @Autowired
    private RestClient restClient;
}
