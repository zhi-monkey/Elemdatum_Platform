package org.dlut.adv.mineai.model.pool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @description 定时任务线程池
 */
@Configuration
@EnableAsync
public class SchduledThreadPool {

    @Bean
    public ThreadPoolTaskScheduler executor() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadNamePrefix("async-scheduled-pool-");
        taskScheduler.setPoolSize(5);
        return taskScheduler;
    }

}
