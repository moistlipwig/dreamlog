# Check Project Phase

Review current project phase status and suggest next steps based on roadmap.

## Instructions

1. **Read Roadmap**
   - Open and analyze: `README.md`
   - Identify all phases and their Definition of Done (DoD)

2. **Check Current Status**
   - Review recent commits: `git log -10 --oneline`
   - Check which features are implemented
   - Verify what's in progress (branches, open PRs)

3. **Assess Phase Completion**

   For each phase, check if DoD criteria are met:

   **Phase 0 (Setup):**
   - [ ] CI/CD pipeline running
   - [ ] Docker Compose with Postgres works
   - [ ] Backend runs: `./gradlew :backend:bootRun`
   - [ ] Frontend runs: `cd frontend && npm start`
   - [ ] Tests pass: `./gradlew :backend:test` and `npm test`

   **Phase 1 (Auth - OIDC):**
   - [ ] Google OAuth configured
   - [ ] `/api/me` endpoint exists
   - [ ] HttpOnly cookie session
   - [ ] Angular auth guards implemented
   - [ ] Logout flow works

   **Phase 2 (CRUD + Search):**
   - [ ] DreamEntry entity and table
   - [ ] CRUD REST API works
   - [ ] FTS with trigrams implemented
   - [ ] Search endpoint < 200ms on 1000 entries
   - [ ] Angular dream list and form

   ... (continue for other phases)

4. **Identify Gaps**
   - What's incomplete in current phase?
   - What's blocking progress?
   - Are there technical debts?

5. **Suggest Next Steps**

   Based on current phase:
   - List 3-5 concrete tasks to complete current phase
   - Prioritize by importance (DoD requirements first)
   - Include acceptance criteria for each task
   - Estimate complexity (simple/medium/complex)

6. **Generate Task List**

   Create actionable tasks:
   ```markdown
   ## Next Steps for Phase X

   ### High Priority (DoD Requirements)
   1. [Task name]
      - Acceptance: [specific criteria]
      - Files: [relevant files]
      - Complexity: [simple/medium/complex]

   ### Medium Priority (Nice to have)
   ...

   ### Technical Debt
   ...
   ```

7. **Validate Dependencies**
   - Check if previous phases are truly complete
   - Identify any prerequisites for next phase
   - Suggest refactoring if needed before advancing

## Output Format

```markdown
# Project Phase Status

## Current Phase: Phase X - [Name]

### Completion Status: Y%

✅ Completed:
- Item 1
- Item 2

🔄 In Progress:
- Item 3 (60% done)

❌ Not Started:
- Item 4
- Item 5

### Blockers
- [Blocker description with suggestion]

### Next 5 Tasks (Prioritized)

1. **[Task Name]** (Complexity: Medium)
   - DoD Requirement: Yes
   - Description: [what needs to be done]
   - Files: [path/to/files]
   - Acceptance: [how to verify completion]
   - Command: [how to test]

2. ...

### Recommended Focus
[Strategic advice on what to tackle first and why]

### Phase Transition
[When this phase will be complete, what next phase requires]
```

## Example Usage

When user types `/phase`, you should:
1. Analyze README.md roadmap
2. Check git history and current branch
3. Scan codebase for implemented features
4. Provide detailed phase status report
5. Suggest prioritized next steps
6. Offer to help with any task

This helps maintain alignment with project goals and systematic progress tracking.
