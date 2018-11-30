package com.wind.blog.svc.config;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ESConfig elasticsearch spring-data 目前支持的最高版本为5.5 所以需要自己注入生成客户端
 *
 * @author qianchun 2018/9/27
 **/
@Component
@Configurable
@PropertySource("classpath:es.properties")
public class ESConfig /* implements FactoryBean<RestHighLevelClient>, InitializingBean, DisposableBean */ {
    private final static Logger logger = LoggerFactory.getLogger(ESConfig.class);

    @Value("${es.host}")
    private String host;

    @Value("${es.port}")
    private int port;

    @Value("${es.schema}")
    private String schema;

    @Value("${es.connect.timeout}")
    private int connectTimeout;

    @Value("${es.socket.timeout}")
    private int socketTimeout;

    @Value("${es.connection.request.timeout}")
    private int connnetctionRequestTimeout;

    @Value("${es.max.connect.num}")
    private int maxConnectNum;

    @Value("${es.max.connect.per.route}")
    private int maxConnectPerRoute;

    @Value("${es.unique.connnect.time.config}")
    private boolean uniqueConnectTimeConfig;

    @Value("${es.unique.connect.num.config}")
    private boolean uniqueConnectNumConfig;

    private static RestClientBuilder builder;

    private static RestHighLevelClient restHighLevelClient;

    private static RestClient restClient;


    /**
     * build client
     * 
     * @return 返回结果
     */
    private RestClientBuilder getBuilder() {
        try {
            if (builder != null) {
                return builder;
            }
            HttpHost httpHost = new HttpHost(host, port, schema);
            RestClientBuilder builder = RestClient.builder(httpHost);
            if (uniqueConnectTimeConfig) {
                builder.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
                    @Override
                    public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder builder) {
                        builder.setConnectTimeout(connectTimeout);
                        builder.setSocketTimeout(socketTimeout);
                        builder.setConnectionRequestTimeout(connnetctionRequestTimeout);
                        return builder;
                    }
                });
            }
            if (uniqueConnectNumConfig) {
                builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                        httpAsyncClientBuilder.setMaxConnTotal(maxConnectNum);
                        httpAsyncClientBuilder.setMaxConnPerRoute(maxConnectPerRoute);
                        return httpAsyncClientBuilder;
                    }
                });
            }

            return builder;
        } catch (Exception e) {
            logger.error("[ESConfig] build client exception : ", e.getMessage());
        }
        return null;
    }

    /**
     * 实例化 restHighLevelClient 对象
     * @return 返回结果
     */
    @Bean
    public RestHighLevelClient getRestHighLevelClient() {
        if (builder == null) {
            builder = getBuilder();
        }
        if(builder==null) {
            return null;
        }
        if (restHighLevelClient == null) {
            restHighLevelClient = new RestHighLevelClient(builder);
        }
        return restHighLevelClient;
    }

    /**
     * 实例化 restclient 对象
     * @return 返回结果
     */
    @Bean
    public RestClient getRestClient() {
        if (builder == null) {
            builder = getBuilder();
        }
        if(builder==null) {
            return null;
        }
        if (restClient == null) {
            restClient = builder.build();
        }
        return restClient;
    }

    /**
     * 关闭连接
     *
     * @throws Exception
     */
    public void destroy() throws Exception {
        try {
            if (restHighLevelClient != null) {
                restHighLevelClient.close();
                restHighLevelClient = null;
            }

            if(restClient != null) {
                restClient.close();
                restClient = null;
            }
        } catch (IOException e) {
            logger.error("[ESConfig] close es client exception : ", e);
        }
    }
}
