package com.example.batchprocessing;

import java.util.Map;
import java.util.UUID;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "warehouse.api.enabled", havingValue = "true")
public class ImportApi {
    private static final Logger log = LoggerFactory.getLogger(ImportApi.class);
    private final JobLauncher launcher;
    private final Job job;
    private final Tracer tracer;

    public ImportApi(JobLauncher launcher, Job job, Tracer tracer) {
        this.launcher = launcher;
        this.job = job;
        this.tracer = tracer;
    }

    @PostMapping("/imports")
    public ResponseEntity<?> launch(@RequestParam(defaultValue = "normal") String sample,
                                    HttpServletRequest request) throws Exception {
        if (!sample.equals("normal") && !sample.equals("invalid")) {
            return ResponseEntity.badRequest().body("sample must be normal or invalid");
        }
        try (var uri = MDC.putCloseable("URI", request.getRequestURI())) {
            log.info("api.received");
            Span processing = tracer.nextSpan().name("warehouse.import").start();
            try (var scope = tracer.withSpan(processing)) {
                var parameters = new JobParametersBuilder().addString("sample", sample)
                    .addString("request", UUID.randomUUID().toString()).toJobParameters();
                var execution = launcher.run(job, parameters);
                var status = execution.getStatus();
                return ResponseEntity.status(status == BatchStatus.COMPLETED ? 200 : 422)
                    .body(Map.of("executionId", execution.getId(), "status", status,
                                 "traceId", processing.context().traceId()));
            } finally {
                processing.end();
            }
        }
    }
}
