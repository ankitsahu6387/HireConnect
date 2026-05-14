# SonarQube Setup

The repository includes a root Maven aggregator in `pom.xml`, so SonarQube can analyze all microservices together.

## Local SonarQube

Start SonarQube on `http://localhost:9000`, create a token, then run:

```powershell
.\notification-service\mvnw.cmd -f pom.xml clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN
```

## Custom SonarQube Server

```powershell
.\notification-service\mvnw.cmd -f pom.xml clean verify sonar:sonar -Dsonar.host.url=http://YOUR_HOST:9000 -Dsonar.token=YOUR_TOKEN
```

## Project Defaults

The root `pom.xml` sets:

- Project key: `hireconnect`
- Project name: `HireConnect`
- Default SonarQube URL: `http://localhost:9000`
- Java version: `17`

Scanner output is ignored through the root `.gitignore`.
