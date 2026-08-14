package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import com.engineering.software_engineering_orchestrator.orchestration.WorkflowGraph;
import com.engineering.software_engineering_orchestrator.orchestration.WorkflowNode;
import org.springframework.stereotype.Component;

@Component
public class PlannerAgent implements EngineeringAgent {

    @Override
    public AgentType getType() {
        return AgentType.PLANNER;
    }

    @Override
    public AgentResult execute(EngineeringState state) {
        String requirement = state.getNormalizedRequirement();

        if (requirement == null || requirement.isBlank()) {
            return AgentResult.failure(
                    "Requirement must be reviewed before planning"
            );
        }

        WorkflowGraph graph = new WorkflowGraph();

        graph.addNode(new WorkflowNode(
                "requirements",
                "Requirement Review",
                AgentType.REQUIREMENT_ANALYST
        ));

        graph.addNode(new WorkflowNode(
                "planning",
                "Planning",
                AgentType.PLANNER
        ));

        graph.addNode(new WorkflowNode(
                "architecture",
                "Architecture Review",
                AgentType.ARCHITECT
        ));

        graph.addNode(new WorkflowNode(
                "implementation",
                "Implementation",
                AgentType.DEVELOPER
        ));

        graph.addNode(new WorkflowNode(
                "testing",
                "Testing",
                AgentType.TESTER
        ));

        graph.addNode(new WorkflowNode(
                "security",
                "Security Review",
                AgentType.SECURITY
        ));

        graph.addNode(new WorkflowNode(
                "validation",
                "Validation",
                AgentType.VALIDATOR
        ));

        graph.addNode(new WorkflowNode(
                "documentation",
                "Documentation",
                AgentType.DOCUMENTATION
        ));

        graph.addNode(new WorkflowNode(
                "release",
                "Release Readiness",
                AgentType.RELEASE
        ));

        graph.addEdge("requirements", "planning");
        graph.addEdge("planning", "architecture");
        graph.addEdge("architecture", "implementation");

        graph.addEdge("implementation", "testing");
        graph.addEdge("implementation", "security");

        graph.addEdge("testing", "validation");
        graph.addEdge("security", "validation");

        graph.addEdge("validation", "documentation");
        graph.addEdge("documentation", "release");

        state.setWorkflowGraph(graph);

        state.addDecision(
                "Created the initial workflow and task dependencies"
        );

        return AgentResult.success("Workflow plan created");
    }
}