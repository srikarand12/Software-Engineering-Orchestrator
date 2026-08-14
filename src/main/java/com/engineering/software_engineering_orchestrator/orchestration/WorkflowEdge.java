package com.engineering.software_engineering_orchestrator.orchestration;

public record WorkflowEdge(
        String fromNode,
        String toNode
) {
}