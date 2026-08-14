package com.engineering.software_engineering_orchestrator.orchestration;

public enum WorkflowStatus {

    CREATED,
    RUNNING,
    WAITING_FOR_APPROVAL,
    COMPLETED,
    REJECTED,
    FAILED,
    SAFE_STOPPED,
    ROLLED_BACK
}