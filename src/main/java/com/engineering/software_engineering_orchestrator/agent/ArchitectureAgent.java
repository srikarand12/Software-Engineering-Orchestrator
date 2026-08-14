package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import org.springframework.stereotype.Component;

@Component
public class ArchitectureAgent implements EngineeringAgent {

    @Override
    public AgentType getType() {
        return AgentType.ARCHITECT;
    }

    @Override
    public AgentResult execute(EngineeringState state) {

        if (state.getWorkflowGraph() == null) {
            return AgentResult.failure("Workflow plan is missing");
        }

        boolean architectureReady = state.getWorkflowGraph()
                .findNode("architecture")
                .map(state.getWorkflowGraph()::dependenciesCompleted)
                .orElse(false);

        if (!architectureReady) {
            return AgentResult.failure(
                    "Architecture review cannot start until planning is complete"
            );
        }

        state.addDecision(
                "Architecture review completed for the current requirement"
        );

        state.addRisk(
                "Changes should preserve existing URL shortener behavior"
        );

        return AgentResult.success("Architecture review completed");
    }
}