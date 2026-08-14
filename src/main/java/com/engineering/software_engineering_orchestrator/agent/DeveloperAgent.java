package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import org.springframework.stereotype.Component;

@Component
public class DeveloperAgent implements EngineeringAgent {

    @Override
    public AgentType getType() {
        return AgentType.DEVELOPER;
    }

    @Override
    public AgentResult execute(EngineeringState state) {

        if (state.getWorkflowGraph() == null) {
            return AgentResult.failure("Workflow plan is missing");
        }

        boolean implementationReady = state.getWorkflowGraph()
                .findNode("implementation")
                .map(state.getWorkflowGraph()::dependenciesCompleted)
                .orElse(false);

        if (!implementationReady) {
            return AgentResult.failure(
                    "Implementation cannot start until architecture review is complete"
            );
        }

        state.addDecision(
                "Implementation changes prepared for the current requirement"
        );

        return AgentResult.success("Implementation completed");
    }
}