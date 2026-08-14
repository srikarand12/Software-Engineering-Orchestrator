package com.engineering.software_engineering_orchestrator.orchestration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkflowNode {

    private String id;
    private String name;
    private AgentType agentType;

    private volatile WorkflowStatus status =
            WorkflowStatus.CREATED;

    private List<String> dependencies =
            new ArrayList<>();

    public WorkflowNode() {
    }

    public WorkflowNode(
            String id,
            String name,
            AgentType agentType) {

        this.id = id;
        this.name = name;
        this.agentType = agentType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public List<String> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies == null
                ? new ArrayList<>()
                : new ArrayList<>(dependencies);
    }

    public void addDependency(String nodeId) {

        if (nodeId == null || nodeId.isBlank()) {
            return;
        }

        if (!dependencies.contains(nodeId)) {
            dependencies.add(nodeId);
        }
    }
}