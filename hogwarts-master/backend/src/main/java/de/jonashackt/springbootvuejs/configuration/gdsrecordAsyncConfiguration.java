package de.jonashackt.springbootvuejs.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class gdsrecordAsyncConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(gdsrecordAsyncConfiguration.class);
    @Bean (name = "taskExecutor")
    public Executor taskExecutor() {
        LOGGER.debug("Creating gdsrecord Async Task Executor");
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("GDSRecord thread-");
        executor.initialize();
        return executor;
    }
}