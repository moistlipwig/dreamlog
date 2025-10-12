# Run Tests

Run project tests with intelligent selection based on context.

## Instructions

1. **Determine Scope**
   - If user mentioned specific file/feature, run only related tests
   - Otherwise, run all tests

2. **Backend Tests**
   - Full suite: `./gradlew :backend:test`
   - Specific test: `./gradlew :backend:test --tests "ClassName"`
   - Without Docker: `./gradlew :backend:test --tests "*controller*"`

3. **Frontend Tests**
   - All tests: `cd frontend && npm test`
   - Specific component: `npm test -- ComponentName`
   - Watch mode: `npm test -- --watch`

4. **Analyze Results**
   - Parse test output for failures
   - Identify root cause of failures
   - Suggest fixes with code examples

## Context Awareness

Before running tests:
- Check recent file changes with `git diff`
- Identify which module was modified (backend/frontend)
- Run tests only for affected module if possible

## Output Format

```
Test Results for [Module]
========================

✅ Passed: X tests
❌ Failed: Y tests
⏭️  Skipped: Z tests

Failed Tests:
1. TestClassName.methodName (file.java:line)
   Error: [error message]
   Fix: [suggested solution]

Total Time: Xs
```
