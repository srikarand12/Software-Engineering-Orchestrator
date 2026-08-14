package com.engineering.software_engineering_orchestrator.agent;

import com.engineering.software_engineering_orchestrator.orchestration.AgentType;
import com.engineering.software_engineering_orchestrator.orchestration.EngineeringState;
import com.engineering.software_engineering_orchestrator.orchestration.ScenarioType;
import org.springframework.stereotype.Component;

@Component
public class RequirementAgent implements EngineeringAgent {

    @Override
    public AgentType getType() {
        return AgentType.REQUIREMENT_ANALYST;
    }

    @Override
    public AgentResult execute(EngineeringState state) {

        String requirement =
                state.getOriginalRequirement();

        if (requirement == null
                || requirement.isBlank()) {

            return AgentResult.failure(
                    "Requirement cannot be empty"
            );
        }

        String normalized =
                requirement.trim()
                        .replaceAll("\\s+", " ");

        state.setNormalizedRequirement(
                normalized
        );

        ScenarioType scenarioType =
                determineScenario(normalized);

        state.setScenarioType(
                scenarioType
        );

        applyScenarioAnalysis(
                state,
                scenarioType
        );

        state.addDecision(
                "Reviewed and normalized the incoming requirement"
        );

        state.addDecision(
                "Scenario classified as " + scenarioType
        );

        return AgentResult.success(
                "Requirement reviewed"
        );
    }

    private ScenarioType determineScenario(
            String requirement) {

        String value =
                requirement.toLowerCase();

        if (isAmbiguous(value)) {
            return ScenarioType.AMBIGUOUS;
        }

        if (isBrownfield(value)) {
            return ScenarioType.BROWNFIELD;
        }

        return ScenarioType.GREENFIELD;
    }

    private boolean isBrownfield(
            String requirement) {

        return requirement.contains("existing")
                || requirement.contains("enhance")
                || requirement.contains("modify")
                || requirement.contains("update")
                || requirement.contains("refactor")
                || requirement.contains("without breaking")
                || requirement.contains("current api")
                || requirement.contains("existing api");
    }

    private boolean isAmbiguous(
            String requirement) {

        boolean containsVagueLanguage =
                requirement.contains("improve")
                        || requirement.contains("better")
                        || requirement.contains("safer")
                        || requirement.contains("easier")
                        || requirement.contains("optimize");

        boolean lacksConcreteFeature =
                !requirement.contains("expiration")
                        && !requirement.contains("analytics")
                        && !requirement.contains("alias")
                        && !requirement.contains("api")
                        && !requirement.contains("click")
                        && !requirement.contains("redirect");

        return containsVagueLanguage
                && lacksConcreteFeature;
    }

    private void applyScenarioAnalysis(
            EngineeringState state,
            ScenarioType scenarioType) {

        switch (scenarioType) {

            case GREENFIELD ->
                    analyzeGreenfield(state);

            case BROWNFIELD ->
                    analyzeBrownfield(state);

            case AMBIGUOUS ->
                    analyzeAmbiguous(state);
        }
    }

    private void analyzeGreenfield(
            EngineeringState state) {

        state.addAssumption(
                "The service is being implemented as a new capability"
        );

        state.addAcceptanceCriterion(
                "A valid long URL can be converted into a short URL"
        );

        state.addAcceptanceCriterion(
                "Short URLs redirect to the original URL"
        );

        state.addAcceptanceCriterion(
                "Expiration behavior is enforced when requested"
        );

        state.addAcceptanceCriterion(
                "Click activity can be tracked for shortened URLs"
        );

        state.addTask(
                "Define the URL shortener API and data model"
        );

        state.addTask(
                "Implement URL creation and redirect behavior"
        );

        state.addTask(
                "Implement expiration handling"
        );

        state.addTask(
                "Implement click analytics"
        );

        state.addTask(
                "Add validation and automated tests"
        );

        state.addRisk(
                "Short-code collisions must be handled safely"
        );
    }

    private void analyzeBrownfield(
            EngineeringState state) {

        state.addAssumption(
                "An existing URL shortener implementation is already running"
        );

        state.addAcceptanceCriterion(
                "Existing URL shortening behavior continues to work"
        );

        state.addAcceptanceCriterion(
                "Existing API contracts remain compatible"
        );

        state.addAcceptanceCriterion(
                "New behavior is covered by regression tests"
        );

        state.addTask(
                "Review the existing URL shortener modules and APIs"
        );

        state.addTask(
                "Identify impacted services, entities, repositories, and controllers"
        );

        state.addTask(
                "Implement the requested enhancement with minimal API impact"
        );

        state.addTask(
                "Run regression and integration tests"
        );

        state.addRisk(
                "Changes may introduce regressions in existing URL behavior"
        );

        state.addRisk(
                "Existing clients may depend on current API contracts"
        );
    }

    private void analyzeAmbiguous(
            EngineeringState state) {

        state.addAmbiguity(
                "The requirement does not clearly define the expected functional change"
        );

        state.addAmbiguity(
                "Success criteria are not specific enough to determine completion"
        );

        state.addAssumption(
                "Existing URL shortening behavior should remain unchanged"
        );

        state.addAcceptanceCriterion(
                "The requirement must be clarified before high-impact implementation changes"
        );

        state.addTask(
                "Identify unclear parts of the requirement"
        );

        state.addTask(
                "Document assumptions and possible interpretations"
        );

        state.addTask(
                "Limit implementation to safe changes until clarification is available"
        );

        state.addRisk(
                "Implementing an unclear requirement may produce unintended behavior"
        );
    }
}