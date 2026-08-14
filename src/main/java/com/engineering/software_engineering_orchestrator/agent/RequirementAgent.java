package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import org.springframework.stereotype.Component;

@Component
public class RequirementAgent implements EngineeringAgent {

    @Override
    public AgentType getType() {
        return AgentType.REQUIREMENT_ANALYST;
    }

    @Override
    public AgentResult execute(EngineeringState state) {
        String requirement = state.getOriginalRequirement();

        if (requirement == null || requirement.isBlank()) {
            return AgentResult.failure("Requirement cannot be empty");
        }

        String normalizedRequirement = requirement.trim();
        state.setNormalizedRequirement(normalizedRequirement);

        state.getDecisionHistory()
                .add("Reviewed and normalized the incoming requirement");

        return AgentResult.success("Requirement reviewed");
    }
}