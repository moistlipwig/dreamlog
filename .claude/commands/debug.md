# Debug Issue

Systematic debugging workflow for backend and frontend issues.

## Instructions

1. **Gather Context**
   - What is the problem? (error message, unexpected behavior)
   - When does it occur? (on startup, specific action, always)
   - Recent changes? Check: `git diff` and `git log -5 --oneline`

2. **Identify Layer**
   - **Backend Error:** Check backend logs, HTTP status codes
   - **Frontend Error:** Check browser console, network tab
   - **Database Error:** Check Flyway migrations, connection
   - **Build Error:** Check Gradle/npm error messages

## Backend Debugging

### Application Won't Start
1. Check if port 8080 is available: `netstat -ano | findstr 8080`
2. Check if database is running: `docker compose ps`
3. Check application logs for exceptions
4. Verify application.properties configuration

### Runtime Error (500, etc.)
1. Find error in logs: check stack trace
2. Identify failing component (Controller/Service/Repository)
3. Check relevant code: `Grep pattern="<ErrorClass>" path="backend/src"`
4. Add logging if needed: `log.error("Context: {}", variable)`
5. Check database state if persistence issue

### Test Failure
1. Run specific test: `./gradlew :backend:test --tests "TestClass"`
2. Check test reports: `backend/build/reports/tests/test/index.html`
3. Common issues:
   - Docker not running (for integration tests)
   - Missing @AutoConfigureMockMvc(addFilters = false)
   - Incorrect mock setup
4. Compare with working test patterns in codebase

## Frontend Debugging

### Application Won't Start
1. Check Node/npm versions: `node -v`, `npm -v`
2. Reinstall dependencies: `rm -rf node_modules && npm ci`
3. Check for port conflicts (4200)
4. Verify proxy configuration: `cat proxy.conf.json`

### Runtime Error
1. Open browser DevTools (F12)
2. Check Console for errors
3. Check Network tab for failed API calls
4. Identify failing component
5. Search code: `Grep pattern="<ErrorMessage>" path="frontend/src"`

### Build/Compilation Error
1. Run typecheck: `npm run typecheck`
2. Common issues:
   - Missing imports in standalone component
   - Incorrect type annotations
   - Using deprecated Angular APIs
3. Check ESLint: `npm run lint`

### Test Failure
1. Run specific test: `npm test -- ComponentName`
2. Common issues:
   - Missing component imports
   - Async operations not handled
   - TestBed not properly configured
3. Check for open handles: `npm test -- --detectOpenHandles`

## Database Debugging

### Migration Failed
1. Check Flyway logs in backend startup
2. Verify migration syntax: SQL validation
3. Check database state: `docker compose exec db psql -U dreamlog -c "\d"`
4. Clean slate if needed: `docker compose down -v && docker compose up -d db`

### Connection Issues
1. Check database is running: `docker compose ps`
2. Test connection: `docker compose exec db psql -U dreamlog -c "SELECT 1;"`
3. Verify credentials in application.properties
4. Check network: `docker network ls` and `docker compose logs db`

## Systematic Debugging Steps

1. **Reproduce** - Ensure you can consistently reproduce the issue
2. **Isolate** - Narrow down to specific component/method
3. **Inspect** - Check relevant logs, variables, state
4. **Hypothesize** - Form theory about root cause
5. **Test** - Verify hypothesis with targeted changes
6. **Fix** - Implement proper solution
7. **Verify** - Run tests, manual verification
8. **Document** - Add tests to prevent regression

## Common Issues Reference

### Backend
- Port conflict → Kill process or change port
- Docker not running → Start Docker Desktop
- Flyway checksum mismatch → Clean database or fix migration
- Test fails → Check if Docker running, mock setup correct

### Frontend
- Module not found → npm ci
- Proxy not working → Check backend running, proxy.conf.json
- Component test fails → Add missing imports
- Build fails → Check TypeScript errors with npm run typecheck

### Database
- Connection refused → docker compose up -d db
- Migration fails → Check SQL syntax, verify clean state
- Query slow → Check indexes, EXPLAIN ANALYZE query

## Output Format

Provide:
1. **Issue Summary** - What's broken
2. **Root Cause** - Why it's broken (with file:line references)
3. **Fix** - Code changes needed with examples
4. **Verification** - Commands to verify fix works
5. **Prevention** - How to avoid this in future (tests, patterns)
