# Wiki Operations Log

**Summary**: Append-only log recording all operations, ingestions, and status updates in the wiki.  
**Sources**: Initial wiki setup  
**Last updated**: 2026-10-14  

---

## 2026-10-14

### Guide: Step-by-Step OAuth Token Preparation (Google, Discord, GitHub)
- **Source**: User request
- **Action**: Created step-by-step developer guide for configuring OAuth2 applications, client credentials, callback URIs, scopes, and testing/exchanging tokens across Google, Discord, and GitHub.
- **Created**:
  - `document/wiki/patterns/security/oauth-token-preparation.md`
- **Updated**:
  - `document/wiki/index.md`
  - `document/wiki/concepts/spring-security.md`
  - `document/wiki/log.md`

### Feature: Configure OAuth2 (Google, Discord, GitHub) Combined with Existing JWT Refresh Token
- **Source**: User request
- **Action**: Added `spring-boot-starter-oauth2-client` and implemented stateless OAuth2 login (Google, GitHub, Discord) combined with `JwtTokenService` single-use refresh token rotation while preserving non-OAuth signing options.
- **Created**:
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/OAuth2Properties.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/HttpCookieOAuth2AuthorizationRequestRepository.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/OAuth2UserInfo.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/GoogleOAuth2UserInfo.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/GithubOAuth2UserInfo.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/DiscordOAuth2UserInfo.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/OAuth2UserInfoFactory.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/OAuth2UserProvisioner.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/DefaultOAuth2UserProvisioner.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/OAuth2AuthenticationSuccessHandler.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/OAuth2AuthenticationFailureHandler.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/oauth2/OAuth2Configuration.java`
  - `surtek-security/src/test/java/com/sabarceria/surtek/oauth2/OAuth2IntegrationTest.java`
- **Updated**:
  - `surtek-security/build.gradle` (added `spring-boot-starter-oauth2-client`)
  - `surtek-security/src/main/java/com/sabarceria/surtek/config/SecurityConfig.java` (wired `oauth2Login` conditionally)
  - `surtek-security/src/main/java/com/sabarceria/surtek/filter/FilterBypassRegistry.java` (auto-bypassed `/oauth2/**` and `/login/oauth2/**` for `JwtAuthenticationFilter`)
  - `document/wiki/concepts/spring-security.md`
  - `document/wiki/log.md`

### Refactor: Remove AllowAuth and AllowSignature in Favor of BypassFilter
- **Source**: User request
- **Action**: Completely deleted `@AllowAuth` and `@AllowSignature` annotations; unified endpoint filter bypasses onto `@BypassFilter`.
- **Deleted**:
  - `surtek-security/src/main/java/com/sabarceria/surtek/model/annotation/AllowAuth.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/model/annotation/AllowSignature.java`
- **Updated**:
  - `surtek-security/src/main/java/com/sabarceria/surtek/filter/FilterBypassRegistry.java`
  - `surtek-security/src/main/java/com/sabarceria/surtek/model/annotation/BypassFilter.java`
  - `surtek-security/src/test/java/com/sabarceria/surtek/filter/FilterRegistryTest.java`
  - `document/wiki/concepts/spring-security.md`
  - `document/wiki/log.md`

## 2026-10-13

### Initial Wiki Setup
- **Source**: Initial setup
- **Action**: Initialized the LLM Wiki folder structure with base directory set to `document/`.
- **Created**:
  - `document/raw/` and `document/raw/tasks/`
  - `document/wiki/patterns/`
  - `document/wiki/decisions/`
  - `document/wiki/concepts/`
  - `document/wiki/tasks/backlog.md`
  - `document/wiki/index.md`
  - `document/wiki/log.md`

### Ingestion: Spring Security Architecture & Usage
- **Source**: `surtek-security/` module
- **Action**: Created concept page for Spring Security architecture and usage; moved documentation from `surtek-security/WIKI.md`.
- **Created**:
  - `document/wiki/concepts/spring-security.md`
- **Updated**:
  - `document/wiki/index.md`

### Tooling: Graphify and Ponytail Setup
- **Source**: User request / agent integration
- **Action**: Installed and configured Graphify and Ponytail for the repository and AI agents.
- **Created**:
  - `graphify-out/` with AST graph, HTML visualizer, and report
  - `.agents/rules/graphify.md`, `.agents/rules/ponytail.md`
  - `.agents/workflows/graphify.md`, `.agents/workflows/ponytail.md`, `.agents/workflows/ponytail-review.md`, `.agents/workflows/ponytail-audit.md`
  - `.agents/skills/` (ponytail, ponytail-review, ponytail-audit, ponytail-debt, ponytail-gain, ponytail-help)
  - `document/wiki/concepts/graphify.md`
  - `document/wiki/concepts/ponytail.md`
- **Updated**:
  - `document/wiki/index.md`

## Related pages

- [[index]]
- [[tasks/backlog]]
- [[concepts/graphify]]
- [[concepts/ponytail]]
