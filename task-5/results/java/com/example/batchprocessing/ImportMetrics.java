package com.example.batchprocessing;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component
@ManagedResource(objectName = "tradeware:type=Import")
public class ImportMetrics implements JobExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(ImportMetrics.class);
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong written = new AtomicLong();
    private volatile double lastSeconds;

    @ManagedAttribute public long getCompleted() { return completed.get(); }
    @ManagedAttribute public long getFailed() { return failed.get(); }
    @ManagedAttribute public long getWritten() { return written.get(); }
    @ManagedAttribute public double getLastSeconds() { return lastSeconds; }

    @Override
    public void beforeJob(JobExecution job) {
        log.atInfo().addKeyValue("executionId", job.getId()).log("import.started");
    }
    @Override
    public void afterJob(JobExecution job) {
        long rows = job.getStepExecutions().stream().mapToLong(step -> step.getWriteCount()).sum();
        written.addAndGet(rows);
        lastSeconds = Duration.between(job.getStartTime(), job.getEndTime()).toMillis() / 1000.0;
        if (job.getStatus() == BatchStatus.COMPLETED) {
            completed.incrementAndGet();
        } else {
            failed.incrementAndGet();
        }
        log.atInfo().addKeyValue("executionId", job.getId()).addKeyValue("rows", rows)
            .addKeyValue("status", job.getStatus()).addKeyValue("seconds", lastSeconds)
            .log("import.finished");
    }
}
