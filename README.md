# Software Engineering Orchestrator

This is a Spring Boot project I built to model a software engineering workflow using a set of specialized agents.

A requirement moves through requirement analysis, planning, architecture, development, testing, security review, validation, documentation, and release readiness.

The workflow is dependency-driven rather than just running every step in sequence. Testing and security checks run in parallel after development, and validation waits for both of them to finish before continuing.

## What it does

- Processes software requirements through multiple engineering agents
- Classifies requirements as greenfield, brownfield, or ambiguous
- Uses a workflow graph to manage dependencies between stages
- Breaks requirements into engineering tasks and acceptance criteria
- Runs testing and security checks in parallel
- Waits for both parallel checks before validation
- Applies validation, security, release, and change-control checks
- Handles failures and timeouts
- Limits retries to avoid uncontrolled execution
- Uses a fallback path when parallel checks cannot complete
- Supports safe-stop when a workflow should not continue
- Supports rollback before release
- Supports resuming a workflow from persisted state
- Re-plans the workflow when the requirement changes
- Stores workflow state and audit history in H2
- Records assumptions, risks, decisions, and validation results
- Tracks basic workflow reliability metrics
- Requires human approval before release
- Produces a final engineering summary
- Includes a URL shortener as the working engineering example
- Includes integration tests for the main workflow scenarios

## Workflow

The main workflow looks like this:

```text
Requirement Analysis
        |
     Planning
        |
   Architecture
        |
  Development
      /    \
 Testing  Security
      \    /
    Validation
        |
  Documentation
        |
 Release Readiness
        |
 Human Approval
```

Testing and security review run independently after development.

Validation does not continue until both checks are complete. Release also has a human approval step so the workflow cannot automatically move from engineering completion to final release.

## Scenario Handling

The requirement stage handles three types of input.

### Greenfield

Greenfield represents a new application or capability.

For example:

```text
Build a URL shortener with expiration and click analytics
```

The workflow identifies the requirement as a new implementation and creates the engineering tasks, acceptance criteria, risks, and workflow dependencies needed to process it.

### Brownfield

Brownfield represents a change to an existing application.

For example:

```text
Add custom aliases to the existing URL shortener without breaking current APIs
```

In this case the workflow considers the existing behavior and adds backward compatibility as part of the change-control checks.

### Ambiguous

Ambiguous requirements do not provide enough detail to safely make every engineering decision.

For example:

```text
Improve the URL shortener
```

Instead of silently making high-impact decisions, the workflow records ambiguities and assumptions and keeps those decisions visible for review.

## Workflow State

Each execution has its own workflow state.

The state keeps information such as:

- Execution ID
- Original requirement
- Normalized requirement
- Scenario type
- Assumptions
- Ambiguities
- Acceptance criteria
- Engineering tasks
- Risks
- Validation results
- Decision history
- Workflow graph
- Current workflow status
- Retry information
- Guardrail checks
- Reliability metrics
- Human approval status
- Final engineering summary

The state is persisted so a workflow can be inspected or resumed later.

## Parallel Processing

Testing and security review are independent after development, so they run in parallel.

The implementation uses `CompletableFuture` with an `ExecutorService` for this.

The workflow waits for both operations to complete before entering validation.

This gives the workflow an explicit synchronization point:

```text
Development
    |
    +--------+
    |        |
 Testing  Security
    |        |
    +--------+
        |
    Validation
```

A timeout prevents the workflow from waiting indefinitely for either parallel operation.

## Failure Handling

The workflow includes several controls for handling failures.

### Bounded Retries

Retries are limited to three attempts.

This prevents a failed workflow from repeatedly executing without a limit.

### Fallback

If the parallel testing and security execution cannot complete successfully, the workflow can fall back to running those checks sequentially.

The fallback is recorded in the workflow state and audit history.

### Safe-stop

A workflow can be safely stopped when it should not continue.

Safe-stop preserves the current workflow state rather than continuing into later stages.

### Rollback

Rollback is available before final release.

For this prototype, rollback represents moving the workflow back to a safe pre-release state. It does not attempt to roll back an application that has already been deployed to a production environment.

### Resume

Persisted workflows can be resumed without creating an entirely new workflow.

If a workflow is already waiting for human approval, resume returns the existing state instead of unnecessarily running the engineering stages again.

## Dynamic Re-planning

Requirements can change while engineering work is being evaluated.

The re-plan operation accepts an updated requirement, clears the parts of the workflow that depend on the previous requirement, and runs requirement analysis and planning again.

For example, a workflow can start with:

```text
Build a URL shortener
```

and later change to:

```text
Add custom aliases to the existing URL shortener without breaking current APIs
```

The updated requirement is analyzed again and can result in a different scenario classification, tasks, risks, assumptions, and workflow plan.

The re-plan operation is also recorded in the workflow history.

## Guardrails

The orchestrator applies a small set of checks before allowing the workflow to move into later stages.

The current checks include:

- Testing must complete before validation
- Security review must complete before validation
- Documentation must complete before release
- Release requires human approval
- Retries are limited to three attempts
- Brownfield changes include backward compatibility checks
- Ambiguous requirements are treated cautiously until they are clarified

These checks are intentionally visible in the workflow state instead of being hidden inside individual agents.

## Human Approval

Release is not automatic.

After all engineering stages finish, the workflow moves to:

```text
WAITING_FOR_APPROVAL
```

At that point a reviewer can approve or reject the workflow.

Approval changes the workflow to:

```text
COMPLETED
```

Rejection changes it to:

```text
REJECTED
```

The approval decision is persisted and added to the audit history.

## Persistence and Audit History

Workflow state is stored using Spring Data JPA and H2.

The persisted state allows the application to retain information such as:

- Current workflow status
- Workflow graph
- Retry count
- Requirements
- Tasks
- Decisions
- Risks
- Assumptions
- Validation results
- Guardrail results
- Metrics

Important workflow events are also stored separately in the audit history.

Examples include:

```text
WORKFLOW_STARTED
WAITING_FOR_APPROVAL
WORKFLOW_APPROVED
WORKFLOW_REJECTED
WORKFLOW_RETRIED
WORKFLOW_RESUMED
WORKFLOW_SAFE_STOPPED
WORKFLOW_ROLLED_BACK
WORKFLOW_REPLANNED
FALLBACK_ACTIVATED
```

This makes it possible to see both the current state and how the workflow reached that state.

## Metrics

The workflow records a small set of reliability and execution metrics.

These include:

- Retry count
- Failure count
- Fallback count
- Rollback count
- Re-plan count
- End-to-end latency
- Recovery time
- Success rate

The metrics are intentionally lightweight for this prototype and are stored as part of the workflow state.

## Final Engineering Summary

Before the workflow reaches the human approval stage, it creates a final engineering summary.

The summary includes information collected during execution, including:

- Scenario type
- Engineering plan
- Tasks
- Validation results
- Risks
- Assumptions
- Produced artifacts
- Known limitations

This provides a concise view of what the workflow decided and what should be reviewed before approval.

## URL Shortener

The project includes a URL shortener as the working application used by the orchestration scenarios.

It supports:

- Creating short URLs
- Redirecting to the original URL
- URL expiration
- Click tracking
- Custom aliases
- Duplicate alias validation
- Expired URL handling
- Inactive URL handling

This gives the orchestrator a concrete application to use for greenfield and brownfield engineering scenarios.

## Trade-offs

- H2 is used so the project can run locally without any external database setup.
- The workflow runs inside the Spring Boot application instead of using a separate workflow engine. This keeps the prototype simple while still showing dependencies, parallel execution, retries, and recovery.
- Testing and security checks run with `CompletableFuture` and `ExecutorService`. For a larger system, these could be moved to a distributed execution or messaging platform.
- Retries are limited to avoid workflows running repeatedly without control.
- If the parallel testing and security checks fail, the workflow falls back to running them sequentially.
- Rollback is handled at the workflow level before release. It does not represent rollback of an already deployed production application.
- Safe-stop is used when the workflow should not continue and keeps the current state available for review.
- Workflow metrics are stored with the workflow state. In a production system, these would normally be sent to a monitoring platform.
- Re-planning resets the affected workflow state when the requirement changes and runs the workflow again using the updated requirement.
- Human approval is handled through API calls instead of an external approval or change-management system.
- The guardrails are implemented directly in the orchestration logic rather than through a separate policy engine.
- The main goal of the project is to demonstrate controlled workflow orchestration, recovery, traceability, and human approval without adding unnecessary infrastructure.

## Tech Stack

- Java 21
- Spring Boot
- Spring Data JPA
- Hibernate
- H2
- Maven
- CompletableFuture
- ExecutorService
- JUnit
- GitHub Actions

## Run the Application

From the project directory:

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

## Run Tests

```powershell
.\mvnw.cmd test
```

## Build

```powershell
.\mvnw.cmd clean package
```

## Main Workflow APIs

Create a workflow:

```text
POST /api/v1/workflows
```

Get workflow state:

```text
GET /api/v1/workflows/{executionId}
```

Get audit history:

```text
GET /api/v1/workflows/{executionId}/history
```

Approve or reject:

```text
POST /api/v1/workflows/{executionId}/approve
POST /api/v1/workflows/{executionId}/reject
```

Recovery operations:

```text
POST /api/v1/workflows/{executionId}/retry
POST /api/v1/workflows/{executionId}/resume
POST /api/v1/workflows/{executionId}/safe-stop
POST /api/v1/workflows/{executionId}/rollback
```

Re-plan after a requirement change:

```text
POST /api/v1/workflows/{executionId}/replan
```

## Testing

The integration tests cover the main workflow behavior, including:

- Workflow creation
- Human approval
- Human rejection
- Retry handling
- Maximum retry enforcement
- Workflow resume
- Audit history
- Greenfield requirements
- Brownfield requirements
- Ambiguous requirements
- Safe-stop
- Rollback
- Dynamic re-planning
- Guardrails
- Metrics
- Final engineering summary

The test suite runs through Maven and is also executed by GitHub Actions for changes pushed to the repository.

## CI

GitHub Actions is configured to build and test the project automatically.

The CI workflow:

1. Checks out the repository
2. Sets up Java 21
3. Makes the Maven wrapper executable on the Linux runner
4. Runs the Maven test suite

This provides a repeatable check that the application still compiles and the integration tests pass.

## Limitations

This is a prototype, so there are a few intentional limitations.

- H2 is used instead of an external production database.
- Workflow execution happens inside a single application instance.
- Metrics are stored with workflow state instead of being exported to an observability platform.
- Human approval is exposed through REST APIs rather than a separate approval UI.
- Rollback operates on workflow state and does not perform infrastructure or production deployment rollback.
- Guardrails are implemented in application code instead of using an external policy engine.
- The agents simulate engineering responsibilities and do not call external AI models or development tools.
- The project focuses on orchestration behavior rather than production-scale infrastructure.

## Project Goal

The goal of this project is to show how a software engineering process can be coordinated through an explicit workflow instead of treating each engineering activity as an isolated step.

The implementation demonstrates requirement handling, dependency management, parallel execution, synchronization, state persistence, failure recovery, controlled retries, fallback behavior, rollback, safe-stop, dynamic re-planning, guardrails, auditability, reliability metrics, and human approval before release.
