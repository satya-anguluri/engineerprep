# Spring Security Filter Chain Demo

Demonstrates how Spring Security's filter chain intercepts HTTP requests, performs authentication, and enforces authorization — **before** the request ever reaches a controller.

## What It Shows

| Concept | Where in Code |
|---|---|
| `DelegatingFilterProxy` → `FilterChainProxy` | Auto-configured by Spring Boot |
| Two independent `SecurityFilterChain` beans | `SecurityConfig.java` |
| JWT authentication for `/api/**` | `JwtAuthFilter.java` + `ApiSecurityChain` |
| Session/form-login for `/admin/**` | `AdminSecurityChain` in `SecurityConfig.java` |
| Public endpoints — no auth | `PublicSecurityChain` in `SecurityConfig.java` |
| Custom `AuthenticationProvider` | `DemoAuthenticationProvider.java` |
| `SecurityContextHolder` usage | `JwtAuthFilter.java`, `DemoAuthenticationProvider.java` |
| Per-request filter logging | `FilterChainLoggingFilter.java` |
| `ExceptionTranslationFilter` behaviour | Triggered automatically on 401/403 |

## Project Structure

```
src/main/java/com/example/security/
  DemoApplication.java
  config/
    SecurityConfig.java          # Two SecurityFilterChain beans
  filter/
    FilterChainLoggingFilter.java # Logs filter chain entry/exit
    JwtAuthFilter.java           # Extracts Bearer token, sets SecurityContext
  auth/
    DemoAuthenticationProvider.java # Custom AuthenticationProvider
    DemoUserDetailsService.java     # In-memory users
  controller/
    ApiController.java
    AdminController.java
    PublicController.java
```

## How to Run

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

## Try It

### 1. Public endpoint — no auth needed
```bash
curl http://localhost:8080/public/hello
# → {"message":"Hello from public endpoint — no auth required"}
```

### 2. API endpoint — JWT required
```bash
# Get a token (base64 of "user:password" used as a demo JWT stand-in)
curl -H "Authorization: Bearer demo-token-for-user" \
     http://localhost:8080/api/profile
# → {"user":"user","roles":["ROLE_USER"],"message":"Authenticated via JWT filter chain"}

# No token → 401
curl http://localhost:8080/api/profile

# Wrong role → 403
curl -H "Authorization: Bearer demo-token-for-user" \
     http://localhost:8080/api/admin-only
```

### 3. Admin endpoint — form login (Basic Auth for demo convenience)
```bash
curl -u admin:adminpass http://localhost:8080/admin/dashboard
# → {"message":"Welcome to admin dashboard","user":"admin"}

# Wrong credentials → 401
curl -u user:wrong http://localhost:8080/admin/dashboard
```

### 4. Watch the filter chain in the logs
Every request prints the active filter chain and the authentication result:
```
[FilterChainLoggingFilter] >>> REQUEST: GET /api/profile  matched chain: API-JWT-Chain
[JwtAuthFilter] Extracted token: demo-token-for-user
[DemoAuthenticationProvider] Authenticating principal: user
[FilterChainLoggingFilter] <<< RESPONSE: 200  principal: user  authorities: [ROLE_USER]
```

## Key Takeaways

1. **First-wins semantics** — Spring iterates `SecurityFilterChain` beans in `@Order` order; only the *first* matching chain runs.
2. **Isolation** — the `/api/**` chain has no session, no CSRF, only JWT. The `/admin/**` chain has sessions and HTTP Basic. They share zero state.
3. **Filter, not MVC** — security is enforced in the servlet filter layer, so it applies even if `DispatcherServlet` never runs.
4. **`SecurityContextHolder`** — `JwtAuthFilter` stores the `Authentication` in thread-local storage; the controller retrieves it via `SecurityContextHolder.getContext().getAuthentication()`.
5. **`ExceptionTranslationFilter`** — converts `AccessDeniedException` → 403 and `AuthenticationException` → 401 automatically.
