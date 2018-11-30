package com.wind.blog.svc.source.csdn;

import com.wind.blog.model.Blog;
import com.wind.blog.svc.model.emun.BlogSource;
import com.wind.blog.svc.model.emun.BlogStatus;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CSDNParser 解析器
 *
 * @author qianchun 2018/8/28
 **/
public class CSDNParser {
    private final static Logger logger = LoggerFactory.getLogger(CSDNParser.class);

    public static final List<String> catalogList = new ArrayList<>();

    static {
        catalogList.add("java");
        catalogList.add("linux");
    }

    /**
     * 获取分页URL
     * @param catalog 分类
     * @param pageNum 分页
     * @return 返回结果
     */
    public static String getUrl(String catalog, int pageNum) {
        return "https://www.aliyun.com/jiaocheng/" + catalog + "-" + pageNum + ".html";
    }

    /**
     * url 解析 blog
     *
     * @param url 链接
     * @return 返回结果
     */
    public static Blog getBlogFromUrl(String url) {
        Blog blog = new Blog();
        blog.setUid(0L);
        blog.setSource(BlogSource.CSDN.getValue());
        blog.setStatus(BlogStatus.PUBLISH.getValue());
        blog.setCreateTime(new Date());
        blog.setUpdateTime(new Date());
        blog.setPublishTime(new Date());
        try {
            Document doc = Jsoup.connect(url).get();
            Elements titleEle = doc.select("ul.article > li.zhinan");
            if (titleEle != null && titleEle.text() != null) {
                blog.setTitle(titleEle.text().trim());
            } else {
                return null;
            }

            Elements summaryEle = doc.select("ul.article > li.li3");
            if (summaryEle != null && summaryEle.text() != null) {
                summaryEle.select("span").remove();
                blog.setSummary(summaryEle.text());
            } else {
                return null;
            }

            Elements contentEle = doc.select("ul.article > li.article-content");
            if (contentEle != null && contentEle.text() != null) {
                blog.setContent(contentEle.text().trim());
            } else {
                return null;
            }
        } catch (IOException e) {
            logger.error("操作异常");
        }

        return blog;
    }

    /**
     * 解析 blog URL
     *
     * @param url 当前URL
     * @return 返回结果
     */
    public static List<String> getBlogURLFromPage(String url) {
        List<String> blogUrlList = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("ul.content-ul > li > div > span > a"); //
            for (Element e : links) {
                if (StringUtils.isNotEmpty(e.attr("href"))) {
                    blogUrlList.add(e.attr("href"));
                }
            }
            return blogUrlList;
        } catch (IOException e) {
            logger.error("查询异常");
        }
        return blogUrlList;
    }

    /**
     * 正则处理 HTML 中指定标签及标签内容
     *
     * @param content 内容
     * @param tag 标签
     * @return 返回结果
     */
    private static String removeTag(String content, String tag) {
        if (StringUtils.isEmpty(content) || StringUtils.isEmpty(tag)) {
            return content;
        }

        String regx = "<" + tag + "[ a-zA-Z0-9=_\"]*>(.*?)</" + tag + ">|<" + tag + "/>";
        return content.replaceAll(regx, "");
    }
}
