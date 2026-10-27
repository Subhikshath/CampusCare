# CampusCare AI

CampusCare is a Spring Boot API and React/Vite web app for reporting and managing campus complaints.

## Run locally on Windows

Prerequisites: Java 17, Node.js 20+, and npm. The default `local` Spring profile uses the H2 database at `data/campuscare-local`; MySQL can be selected by setting `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` in a root `.env` file.

Open two terminals from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location frontend
npm install
npm run dev
```

The frontend is at http://localhost:5173 and the backend health check is http://localhost:8080/health. Local demo accounts are available from the login page; demo account shortcuts are included only in development builds.

Complaint intake asks follow-up questions before assigning a department whenever the issue or its campus location is unclear. Students should identify the building/block and, where relevant, floor/room; for hostels, the hostel name; and for outdoor issues, the named ground or parking area. Reports are not routed until the AI supplies a specific location that matches the student's own messages. After assignment, the department admin assigns a department worker, marks the report in progress, and marks it completed. Students can open their report to see the details, conversation, current status, assigned team/worker, and status history; administrators can inspect the same details across campus.

The authenticated CampusCare Assistant answers portal-use questions using a curated portal guide and Gemini; it does not search complaint records or answer unrelated questions. Each user's chat messages and assistant replies are saved in the configured database and restored when that user reopens the chat. Questions outside the guide are declined without being sent to Gemini. Configure a valid `GEMINI_API_KEY` locally or in the deployment secret manager for AI answers.

### Gemini API troubleshooting

For local development, set `GEMINI_API_KEY` in the repository-root `.env` file. The local Spring profile imports that file. On Windows, process and user environment variables take precedence over `.env`; remove or update any older `GEMINI_API_KEY` or `GEMINI_MODEL` environment variable, open a new terminal, and restart Spring Boot after changing the key. Never put the key in `frontend/.env`, JavaScript, or a committed file.

The default model is `gemini-3.1-flash-lite`. Set `GEMINI_MODEL` only to a model available to the Google Cloud project linked to the key. The key must be permitted to call the Generative Language API. Google currently rejects unrestricted standard Gemini keys; use an AI Studio authorization key or apply an API restriction that allows the Gemini API. Do not apply browser referrer restrictions to a server-side key.

CampusCare reports actionable guidance for common authentication, API enablement, key-restriction, model, quota, and temporary-unavailability errors. Temporary Gemini HTTP 503 responses are retried twice with a short backoff; if Google remains unavailable, retry the report or chat request later. Check [Google's API key guide](https://ai.google.dev/gemini-api/docs/api-key) and [troubleshooting guide](https://ai.google.dev/gemini-api/docs/troubleshooting) for project-side setup and quota information.

## Production build and deployment

Build the API JAR and static frontend bundle:

```powershell
.\mvnw.cmd clean package
Set-Location frontend
npm ci
npm run build
```

Deploy `target/campuscare-ai-1.0.0.jar` to a Java 17 runtime and publish `frontend/dist` on a static web host. Configure the backend with:

- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` for a MySQL database
- `JWT_SECRET` set to a unique, random secret of at least 32 characters
- `CORS_ALLOWED_ORIGIN_PATTERNS` set to the exact frontend origin(s), comma-separated
- `GEMINI_API_KEY` if AI classification and chat should use Gemini
- `INITIAL_ADMIN_NAME`, `INITIAL_ADMIN_EMAIL`, and `INITIAL_ADMIN_PASSWORD` to create the first administrator if the production database has none. The initial password must be at least 12 characters; it is hashed before storage. These values can be removed after the account is provisioned.

For a same-origin deployment, route the frontend's API paths to the backend and leave `VITE_API_URL` unset. For separately hosted frontend and API, set `VITE_API_URL` to the backend's public origin **at frontend build time**; configure `CORS_ALLOWED_ORIGIN_PATTERNS` to allow the frontend origin. Never use `localhost` as a production API URL.

Do not use the local demo profile or its demo accounts in production. Store production secrets in the hosting provider's secret manager, not in source control or frontend environment files.
