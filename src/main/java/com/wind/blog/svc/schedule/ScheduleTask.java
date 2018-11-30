package com.wind.blog.svc.schedule;

import com.wind.blog.svc.model.emun.BlogSource;
import com.wind.blog.svc.service.LinkParseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 任务定时器
 */
@Component
public class ScheduleTask {
    private static Logger logger = LoggerFactory.getLogger(ScheduleTask.class);

    @Autowired
    private LinkParseService linkParseService;

    /**
     * blog link 跑批任务
     */
    @Scheduled(initialDelay = 100, fixedDelay = 60000)
    private void blogTask() {
        try {
            int blogSource = BlogSource.ALIYUN.getValue();
            if (!BlogSource.ALIYUN.getValue().equals(blogSource)) {
                return ;
            }
            linkParseService.start(BlogSource.ALIYUN);
        } catch (Exception e) {
            logger.error("[LINK任务] 录入link异常, 参数: url={");
        }
    }
}
