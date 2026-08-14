package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityAgent implements EngineeringAgent {

    private static final Logger log =
            LoggerFactory.getLogger(SecurityAgent.class);

    @Override
    public AgentType getType() {
        return AgentType.SECURITY;
    }

    @Override
    public AgentResult execute(EngineeringState state) {

        if (state.getWorkflowGraph() == null) {
            return AgentResult.failure("Workflow plan is missing");
        }

        boolean ready = state.getWorkflowGraph()
                .findNode("security")
                .map(state.getWorkflowGraph()::dependenciesCompleted)
                .orElse(false);

        if (!ready) {
            return AgentResult.failure(
                    "Security review cannot start until implementation is complete"
            );
        }

        log.info(
                "Security review started on thread {}",
                Thread.currentThread().getName()
        );

        state.addRisk(
                "URL input and redirect behavior should be validated against unsafe schemes"
        );

        state.addDecision(
                "Security review completed for the current change"
        );

        return AgentResult.success("Security review completed");
    }
}