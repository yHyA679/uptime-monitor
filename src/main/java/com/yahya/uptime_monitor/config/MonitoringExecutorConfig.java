package com.yahya.uptime_monitor.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.time.Clock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(MonitoringExecutionProperties.class)
public class MonitoringExecutorConfig {

    @Bean
    public Clock monitoringClock() {
        return Clock.systemDefaultZone();
    }

    @Bean(name = "monitoringExecutor", destroyMethod = "shutdown")
    public ExecutorService monitoringExecutor(MonitoringExecutionProperties properties) {
        return new ThreadPoolExecutor(
                properties.getWorkerThreads(),
                properties.getWorkerThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.getQueueCapacity()),
                new CustomizableThreadFactory("monitoring-check-"),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
