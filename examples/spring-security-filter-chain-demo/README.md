# Spring Security Filter Chain Demo

This project walks through how Spring Security's filter chain intercepts HTTP requests, performs authentication, and enforces authorization **before** a request reaches any controller.

## What It Shows

| Concept | Where to look |
|---|---|
| `DelegatingFilterProxy` → `FilterChainProxy` wiring | Auto-configured by Spring Boot; `SecurityConfig.java` |
| Multiple `SecurityFilterChain` beans (API vs. Web) | `SecurityConfig.java` |
| Custom `UsernamePasswordAuthenticationFilter` variant | Form-login chain |
| JWT `BearerTokenAuthenticationFilter` equivalent | `JwtAuthenticationFilter.java` |
| `AuthenticationManager` + `AuthenticationProvider` | `SecurityConfig.java` + `CustomUserDetailsService.java` |
| `SecurityContextHolder` populated on success | `JwtAuthenticationFilter.java` |
| `ExceptionTranslationFilter` → 401/403 responses | Automatic; exercised by the curl examples below |
| Reading current principal in a service | `GreetingService.java` |

## Project Layout

```
src/main/java/com/example/security/
  SecurityApplication.java          – Spring Boot entry point
  config/SecurityConfig.java        – Two SecurityFilterChain beans
  filter/JwtAuthenticationFilter.java – Custom JWT filter
  filter/FilterChainLoggingFilter.java– Logs each filter invocation
  controller/AuthController.java    – POST /auth/login  → returns JWT token
  controller/ApiController.java     – GET  /api/hello   (ROLE_USER required)
  controller/AdminController.java   – GET  /api/admin   (ROLE_ADMIN required)
  controller/PublicController.java  – GET  /public/info (no auth)
  service/GreetingService.java      – Reads principal from SecurityContextHolder
  service/CustomUserDetailsService.java
  util/JwtUtil.java                 – Tiny HMAC-SHA256 JWT helper (no extra lib)
```

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

## Try It

### 1. Public endpoint — no token needed
```bash
curl http://localhost:8080/public/info
# → {"message":"This endpoint is public"}
```

### 2. Protected endpoint — no token → 401
```bash
curl http://localhost:8080/api/hello
# → 401 Unauthorized
```

### 3. Obtain a JWT (user: alice / password: password)
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
echo $TOKEN
```

### 4. Access user endpoint with token
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/hello
# → {"message":"Hello, alice! Your roles: [ROLE_USER]"}
```

### 5. alice tries admin endpoint → 403
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin
# → 403 Forbidden
```

### 6. Obtain admin token (user: bob / password: password)
```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"bob","password":"password"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/api/admin
# → {"message":"Welcome to the admin area, bob"}
```

### 7. Watch the filter chain in the logs
Every request prints the ordered filter list to stdout so you can see exactly which filters run.

## Key Interview Takeaways

1. **`DelegatingFilterProxy`** bridges the servlet container (which needs filters at deploy time) with the Spring `ApplicationContext` (where real beans live).
2. **`FilterChainProxy`** holds N `SecurityFilterChain` instances; the *first matching* chain wins — order matters.
3. **Authentication filters** (`JwtAuthenticationFilter`) extract credentials, delegate to `AuthenticationManager → AuthenticationProvider`, then store the result in `SecurityContextHolder`.
4. **`ExceptionTranslationFilter`** sits between authentication/authorization logic and translates `AuthenticationException` → 401 and `AccessDeniedException` → 403, so controllers never see these exceptions.
5. **`SecurityContextHolder`** (ThreadLocal) is the contract: any `@Service` on the same thread can read the current principal without it being passed explicitly.
6. Security happens **before** `DispatcherServlet` — Spring MVC routing cannot bypass it.
