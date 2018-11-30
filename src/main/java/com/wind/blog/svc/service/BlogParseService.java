package com.wind.blog.svc.service;

import com.wind.blog.model.Blog;
import com.wind.blog.model.Link;
import com.wind.blog.svc.common.Constant;
import com.wind.blog.svc.model.emun.BlogSource;
import com.wind.blog.svc.source.aliyun.AliyunParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogParseService {

    private final static Logger logger = LoggerFactory.getLogger(BlogParseService.class);

    @Autowired
    private BlogService blogService;

    @Autowired
    private LinkService linkService;

    /**
     * 解析 BLOG
     *
     * @param url 链接地址
     * @param blogSource blog 来源
     */
    public boolean parseBlog(String url, BlogSource blogSource) {
        try {
            if (StringUtils.isEmpty(url)) {
                logger.error("[BLOG解析] 参数错误, url={}", url);
                return false;
            }

            // 如果URL是 csdn blog 的文章地址, 则爬取文章地址, 并将文章信息录入库中
            Link link = linkService.findByUrl(url);
            if (link == null) {
                link = new Link();
                link.setSource(blogSource.getValue());
                link.setUrl(url);
                link.setIsParse(Constant.LINK_IS_PARSE.YES);

                Blog blog = null;
                if (blogSource == BlogSource.ALIYUN) {
                    blog = AliyunParser.getBlogFromUrl(url);
                } else if (blogSource == BlogSource.CSDN) {

                }

                if (blog == null) {
                    logger.info("[BLOG解析] 解析失败, url={}", url);
                    return false;
                }
                blogService.add(blog);
                link.setBlogId(blog.getId());
                linkService.add(link);
            }
            logger.info("[BLOG解析] 完成, url={}", url);
            return true;
        } catch (Exception e) {
            logger.error("[BLOG解析] 异常, url={}, 异常={}", url, e);
            return false;
        }
    }

//    /**
//     * 解析 BLOG
//     *
//     * @param url 链接地址
//     * @param blogSource blog 来源
//     */
//    public Blog parseBlog(String url, BlogSource blogSource) {
//        try {
//            if (StringUtils.isEmpty(url)) {
//                logger.error("[BLOG解析] 参数错误, url={}", url);
//                return null;
//            }
//
//            // 如果URL是 csdn blog 的文章地址, 则爬取文章地址, 并将文章信息录入库中
//            Link link = linkService.findByUrl(url);
//            if (link == null) {
//                link = new Link();
//                link.setSource(blogSource.getValue());
//                link.setUrl(url);
//                link.setIsParse(Constant.LINK_IS_PARSE.YES);
//
//                Blog blog = null;
//                if (blogSource == BlogSource.ALIYUN) {
//                    blog = AliyunParser.getBlogFromUrl(url);
//                } else if (blogSource == BlogSource.CSDN) {
//
//                }
//                if (blog == null) {
//                    logger.info("[BLOG解析] 解析失败, url={}", url);
//                    return null;
//                }
//                blogService.add(blog);
//                link.setBlogId(blog.getId());
//                linkService.add(link);
//                return blog;
//            }
//            logger.info("[BLOG解析] 完成, url={}", url);
//            return null;
//        } catch (Exception e) {
//            logger.error("[BLOG解析] 异常, url={}, 异常={}", url, e);
//            return null;
//        }
//    }
}
