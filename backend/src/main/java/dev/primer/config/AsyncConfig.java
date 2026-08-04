package dev.primer.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executors for background paper analysis.
 * <p>
 * Deliberately two pools, not one. Each analysis job fans its two agents out and then waits for them;
 * if those subtasks queued behind other jobs on the same fixed pool, a full pool of waiting jobs would
 * deadlock. Jobs get a bounded pool because they hold a database connection and cost real API spend;
 * the agent calls get virtual threads because they are pure I/O waits and are already bounded by the
 * number of jobs that can run at once.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("analysis-");
        // A full queue means the reader will retry on their next access anyway, so dropping the task
        // is preferable to blocking the request thread that published the event.
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor agentExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
