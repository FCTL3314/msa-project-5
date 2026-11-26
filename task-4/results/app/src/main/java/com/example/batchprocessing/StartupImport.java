package com.example.batchprocessing;

import java.util.UUID;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "warehouse.api.enabled", havingValue = "false", matchIfMissing = true)
public class StartupImport implements ApplicationRunner {
    private final JobLauncher launcher;
    private final Job job;

    public StartupImport(JobLauncher launcher, Job job) {
        this.launcher = launcher;
        this.job = job;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var parameters = new JobParametersBuilder().addString("sample", "normal")
            .addString("request", UUID.randomUUID().toString()).toJobParameters();
        var execution = launcher.run(job, parameters);
        if (execution.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException("Startup import failed: " + execution.getId());
        }
    }
}
