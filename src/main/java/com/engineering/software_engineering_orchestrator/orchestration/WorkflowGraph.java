package com.engineering.software_engineering_orchestrator.orchestration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class WorkflowGraph {

    private List<WorkflowNode> nodes = new ArrayList<>();
    private List<WorkflowEdge> edges = new ArrayList<>();

    public WorkflowGraph() {
    }

    public void addNode(WorkflowNode node) {

        if (node == null) {
            return;
        }

        if (findNode(node.getId()).isEmpty()) {
            nodes.add(node);
        }
    }

    public void addEdge(
            String fromNodeId,
            String toNodeId) {

        WorkflowNode fromNode = findNode(fromNodeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Source node not found: " + fromNodeId
                        )
                );

        WorkflowNode toNode = findNode(toNodeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Target node not found: " + toNodeId
                        )
                );

        boolean edgeExists = edges.stream()
                .anyMatch(edge ->
                        edge.fromNode().equals(fromNodeId)
                                && edge.toNode().equals(toNodeId)
                );

        if (edgeExists) {
            return;
        }

        edges.add(
                new WorkflowEdge(
                        fromNode.getId(),
                        toNode.getId()
                )
        );

        toNode.addDependency(
                fromNode.getId()
        );
    }

    public Optional<WorkflowNode> findNode(
            String nodeId) {

        if (nodeId == null || nodeId.isBlank()) {
            return Optional.empty();
        }

        return nodes.stream()
                .filter(node ->
                        node.getId().equals(nodeId)
                )
                .findFirst();
    }

    public boolean dependenciesCompleted(
            WorkflowNode node) {

        if (node == null) {
            return false;
        }

        for (String dependencyId : node.getDependencies()) {

            WorkflowNode dependency =
                    findNode(dependencyId)
                            .orElse(null);

            if (dependency == null
                    || dependency.getStatus()
                    != WorkflowStatus.COMPLETED) {

                return false;
            }
        }

        return true;
    }

    public List<WorkflowNode> getReadyNodes() {

        return nodes.stream()
                .filter(node ->
                        node.getStatus()
                                == WorkflowStatus.CREATED
                )
                .filter(this::dependenciesCompleted)
                .toList();
    }

    public List<WorkflowNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public void setNodes(List<WorkflowNode> nodes) {
        this.nodes = nodes == null
                ? new ArrayList<>()
                : new ArrayList<>(nodes);
    }

    public List<WorkflowEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public void setEdges(List<WorkflowEdge> edges) {
        this.edges = edges == null
                ? new ArrayList<>()
                : new ArrayList<>(edges);
    }
}