# Contributing

## Workflow
1. Create a branch from `main`: `feature/<name>`, `fix/<name>` or `docs/<name>`.
2. Keep each pull request small and about one thing.
3. Run the tests before you push:
   - backend: `cd backend && mvn test`
   - frontend: `cd frontend && npm test`
4. Open a pull request into `main` and link the issue it closes (`Closes #12`).
5. Wait for the CI check (`Java CI with Maven`) to pass.

## Review
Comment on the pull request with:

| Command | What it does |
|---------|--------------|
| `/lgtm` | Says "looks good to me". Does not merge. |
| `/apprv` | Merges the pull request (squash). |
| `/hold` | Puts the pull request on hold, so it cannot be merged. |
| `/unhold` | Releases the hold. |

- You cannot use `/apprv` on your own pull request. Another team member does it.
- Use `/apprv` only after reading the changes, because it merges right away.
- `/apprv` does not work while the pull request is on hold.

## Code
- Add a test for new behaviour.
- Keep comments short and explain why, not what.
- Do not commit generated files such as `target/` or `node_modules/`.
