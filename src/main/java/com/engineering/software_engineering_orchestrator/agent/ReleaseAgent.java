package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import org.springframework.stereotype.Component;

@Component
public class ReleaseAgent implements EngineeringAgent {

    @Override
    public AgentType getType() {
        return AgentType.RELEASE;
    }

    @Override
    public AgentResult execute(EngineeringState state) {

        if (state.getWorkflowGraph() == null) {
            return AgentResult.failure("Workflow plan is missing");
        }

        boolean ready = state.getWorkflowGraph()
                .findNode("release")
                .map(state.getWorkflowGraph()::dependenciesCompleted)
                .orElse(false);

        if (!ready) {
            return AgentResult.failure(
                    "Release review cannot start until documentation is complete"
            );
        }

        state.setHumanApprovalRequired(true);

        state.addDecision(
                "Release checks completed and the workflow is waiting for approval"
        );

        return AgentResult.success("Release is ready for approval");
    }
}