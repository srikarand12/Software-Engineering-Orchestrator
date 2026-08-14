package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import org.springframework.stereotype.Component;

@Component
public class DocumentationAgent implements EngineeringAgent {

    @Override
    public AgentType getType() {
        return AgentType.DOCUMENTATION;
    }

    @Override
    public AgentResult execute(EngineeringState state) {

        if (state.getWorkflowGraph() == null) {
            return AgentResult.failure("Workflow plan is missing");
        }

        boolean ready = state.getWorkflowGraph()
                .findNode("documentation")
                .map(state.getWorkflowGraph()::dependenciesCompleted)
                .orElse(false);

        if (!ready) {
            return AgentResult.failure(
                    "Documentation cannot start until validation is complete"
            );
        }

        state.addDecision(
                "Updated implementation notes and API documentation"
        );

        return AgentResult.success("Documentation completed");
    }
}