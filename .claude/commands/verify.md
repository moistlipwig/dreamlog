# Verify Project

Run full project verification to ensure everything builds and tests pass.

## Tasks

1. **Backend Verification**
   - Run: `./gradlew :backend:build`
   - This includes compilation, tests, and code quality checks
   - Report any failures with specific file paths and line numbers

2. **Frontend Verification**
   - Run: `cd frontend && npm run verify`
   - This runs typecheck, production build, and all tests
   - Report any TypeScript errors, build failures, or test failures

3. **Report Summary**
   - Provide a concise summary of results
   - If any failures, suggest fixes based on error messages
   - If all pass, confirm the project is in good state

## Expected Output

Provide:
- ✅ or ❌ for each verification step
- Total time taken
- Any warnings or errors with file:line references
- Actionable next steps if failures occurred
