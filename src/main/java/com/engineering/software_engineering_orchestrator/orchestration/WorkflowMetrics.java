package com.engineering.software_engineering_orchestrator.orchestration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkflowMetrics {

    private int retryCount;
    private int fallbackCount;
    private int rollbackCount;
    private int replanCount;
    private int failureCount;

    private long endToEndLatencyMs;
    private long mttrMs;

    private double successRate;
}