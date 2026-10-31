# Frontend

Angular web client for the loan workflow. Requires Node 22+.

## Run

```bash
npm install
npm start      # dev server on http://localhost:4200
```

Start the backend first (`cd backend && mvn spring-boot:run`). In development,
calls to `/api/*` are forwarded to the backend on `localhost:8080` (see `proxy.conf.json`),
so the browser never makes a cross-origin request.

## Build and test

```bash
npm run build  # production build in dist/
npm test       # unit tests (Vitest)
```
