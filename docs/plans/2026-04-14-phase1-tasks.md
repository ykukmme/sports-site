# Phase 1 태스크 플랜

작성일: 2026-04-14
기반 설계: docs/plans/2026-04-14-phase1-design.md
상태: 구현 대기

---

## SECTION A — 프로젝트 구조 세팅 (1-1)

## Task 1: Spring Boot 프로젝트 초기화
파일: `D:\study\sports_site\backend\build.gradle`
변경: 신규 생성. dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, spring-boot-starter-actuator, postgresql, flyway-core, jjwt-api, jjwt-impl, jjwt-jackson, spring-boot-starter-test
테스트: 없음 (설정 파일)
의존성: 없음

## Task 2: Spring Boot 메인 설정 파일 생성
파일: `D:\study\sports_site\backend\src\main\resources\application.yml`
변경: 신규 생성. datasource (환경변수 ${POSTGRES_HOST}, ${POSTGRES_PORT}, ${POSTGRES_DB}, ${POSTGRES_USER}, ${POSTGRES_PASSWORD}), jpa.hibernate.ddl-auto=validate, flyway.enabled=true, server.port=${SERVER_PORT:8080}
테스트: 없음 (설정 파일)
의존성: Task 1

## Task 3: Spring Boot 메인 애플리케이션 클래스 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\EsportsApplication.java`
변경: 신규 생성. @SpringBootApplication 클래스 EsportsApplication, main() 메서드
테스트: 없음
의존성: Task 2

## Task 4: 공통 응답 래퍼 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\common\ApiResponse.java`
변경: 신규 생성. 제네릭 클래스 ApiResponse<T>. 필드: boolean success, T data, String message. 정적 팩토리 메서드 ok(T data), fail(String message)
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\common\ApiResponseTest.java::okReturnsSuccessTrue`, `failReturnsSuccessFalse`
의존성: Task 3

## Task 5: 공통 예외 클래스 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\common\exception\BusinessException.java`
변경: 신규 생성. RuntimeException 상속. 필드: String errorCode, String message. 생성자 BusinessException(String errorCode, String message)
테스트: 없음 (POJO)
의존성: Task 3

## Task 6: 전역 예외 핸들러 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\common\exception\GlobalExceptionHandler.java`
변경: 신규 생성. @RestControllerAdvice 클래스. 메서드: handleBusinessException → ApiResponse.fail(), handleMethodArgumentNotValidException → ApiResponse.fail(validation 메시지), handleException → ApiResponse.fail("서버 오류가 발생했습니다.")
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\common\exception\GlobalExceptionHandlerTest.java::handleBusinessExceptionReturns400`, `handleValidationExceptionReturns400`
의존성: Task 4, Task 5

## Task 7: React + Vite 프로젝트 초기화
파일: `D:\study\sports_site\frontend\package.json`
변경: 신규 생성. dependencies: react, react-dom, react-router-dom, axios, @tanstack/react-query. devDependencies: @vitejs/plugin-react, typescript, tailwindcss, @types/react, @types/react-dom
테스트: 없음 (설정 파일)
의존성: 없음

## Task 8: Vite 설정 파일 생성
파일: `D:\study\sports_site\frontend\vite.config.ts`
변경: 신규 생성. server.proxy 설정 — /api → http://backend:8080, /admin → http://backend:8080. VITE_API_BASE_URL 환경변수 참조
테스트: 없음 (설정 파일)
의존성: Task 7

## Task 9: TypeScript 기본 타입 정의 파일 생성
파일: `D:\study\sports_site\frontend\src\types\api.ts`
변경: 신규 생성. 인터페이스 ApiResponse<T>, PageResponse<T> 정의
테스트: 없음 (타입 파일)
의존성: Task 7

## Task 10: axios 인스턴스 생성
파일: `D:\study\sports_site\frontend\src\api\client.ts`
변경: 신규 생성. axios.create() — baseURL: VITE_API_BASE_URL. request interceptor: Authorization 헤더에 localStorage JWT 주입. response interceptor: 401 시 토큰 삭제 + 리다이렉트
테스트: 없음 (인스턴스 설정)
의존성: Task 9

## Task 11: Docker Compose 파일 생성
파일: `D:\study\sports_site\docker-compose.yml`
변경: 신규 생성. services: db (postgres:17, healthcheck: pg_isready, 포트 내부 전용), backend (env_file:.env, 8080, depends_on db service_healthy), frontend (5173). networks: esports-net. volumes: postgres_data
테스트: 없음 (설정 파일)
의존성: Task 2, Task 8

## Task 12: .env.example 업데이트
파일: `D:\study\sports_site\.env.example`
변경: 기존 파일 수정. POSTGRES_HOST=, POSTGRES_PORT=5432, JWT_EXPIRATION_MS=, SERVER_PORT=8080, ADMIN_USERNAME=, ADMIN_PASSWORD= 항목 추가
테스트: 없음 (문서)
의존성: Task 11

---

## SECTION B — DB 스키마 및 Flyway 마이그레이션 (1-2)

## Task 13: Flyway 마이그레이션 — games, teams 테이블
파일: `D:\study\sports_site\backend\src\main\resources\db\migration\V1__init_schema.sql`
변경: 신규 생성. CREATE TABLE games, CREATE TABLE teams (FK → games.id)
테스트: 없음 (SQL 파일)
의존성: Task 2

## Task 14: Flyway 마이그레이션 — players, matches, match_results 테이블
파일: `D:\study\sports_site\backend\src\main\resources\db\migration\V1__init_schema.sql`
변경: 기존 파일에 추가. CREATE TABLE players (team_id nullable). CREATE TABLE matches (CHECK team_a_id != team_b_id, status CHECK IN). CREATE TABLE match_results (match_id UNIQUE)
테스트: 없음 (SQL 파일)
의존성: Task 13

## Task 15: Game 엔티티 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\game\Game.java`
변경: 신규 생성. @Entity @Table(name="games") 클래스 Game
테스트: 없음 (JPA 엔티티)
의존성: Task 14

## Task 16: Team 엔티티 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\team\Team.java`
변경: 신규 생성. @Entity @Table(name="teams"). @ManyToOne Game game. @PreUpdate로 updatedAt 자동 갱신
테스트: 없음 (JPA 엔티티)
의존성: Task 15

## Task 17: Player 엔티티 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\player\Player.java`
변경: 신규 생성. @Entity @Table(name="players"). @ManyToOne(optional=true) Team team
테스트: 없음 (JPA 엔티티)
의존성: Task 16

## Task 18: MatchStatus enum 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\match\MatchStatus.java`
변경: 신규 생성. public enum MatchStatus { SCHEDULED, ONGOING, COMPLETED, CANCELLED }
테스트: 없음 (enum)
의존성: Task 3

## Task 19: Match 엔티티 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\match\Match.java`
변경: 신규 생성. @Entity @Table(name="matches"). @ManyToOne Game, teamA, teamB. @Enumerated(EnumType.STRING) MatchStatus status
테스트: 없음 (JPA 엔티티)
의존성: Task 16, Task 18

## Task 20: MatchResult 엔티티 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\matchresult\MatchResult.java`
변경: 신규 생성. @Entity @Table(name="match_results"). @OneToOne Match match(@JoinColumn unique=true)
테스트: 없음 (JPA 엔티티)
의존성: Task 19

## Task 21: GameRepository 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\game\GameRepository.java`
변경: 신규 생성. interface GameRepository extends JpaRepository<Game, Long>. Optional<Game> findByName(String name)
테스트: 없음
의존성: Task 15

## Task 22: TeamRepository 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\team\TeamRepository.java`
변경: 신규 생성. List<Team> findByGameId(Long gameId), Optional<Team> findByExternalId(String externalId)
테스트: 없음
의존성: Task 16

## Task 23: PlayerRepository 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\player\PlayerRepository.java`
변경: 신규 생성. List<Player> findByTeamId(Long teamId), Optional<Player> findByExternalId(String externalId)
테스트: 없음
의존성: Task 17

## Task 24: MatchRepository 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\match\MatchRepository.java`
변경: 신규 생성. Page<Match> findByStatusAndGameId(...), List<Match> findByStatusAndScheduledAtAfter(...), Optional<Match> findByExternalId(String externalId)
테스트: 없음
의존성: Task 19

## Task 25: MatchResultRepository 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\matchresult\MatchResultRepository.java`
변경: 신규 생성. Optional<MatchResult> findByMatchId(Long matchId)
테스트: 없음
의존성: Task 20

---

## SECTION C — JWT 인증 인프라 (1-4 선행)

## Task 26: JwtProperties 설정 클래스 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\config\JwtProperties.java`
변경: 신규 생성. @ConfigurationProperties(prefix="jwt"). 필드: String secret (→ ${JWT_SECRET}), long expirationMs (→ ${JWT_EXPIRATION_MS}). 하드코딩 없음
테스트: 없음
의존성: Task 3

## Task 27: application.yml JWT 설정 추가
파일: `D:\study\sports_site\backend\src\main\resources\application.yml`
변경: 기존 파일 수정. jwt.secret: ${JWT_SECRET}, jwt.expiration-ms: ${JWT_EXPIRATION_MS} 추가
테스트: 없음
의존성: Task 26

## Task 28: JwtTokenProvider 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\config\JwtTokenProvider.java`
변경: 신규 생성. @Component. 메서드: generateToken(String username), getUsernameFromToken(String token), validateToken(String token). jjwt HS256 서명. 시크릿은 JwtProperties에서만 주입
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\config\JwtTokenProviderTest.java::generateAndValidateToken`, `expiredTokenReturnsFalse`, `invalidSignatureReturnsFalse`
의존성: Task 26, Task 27

## Task 29: JwtAuthenticationFilter 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\config\JwtAuthenticationFilter.java`
변경: 신규 생성. OncePerRequestFilter 상속. Authorization Bearer 토큰 추출 → validateToken → SecurityContextHolder 설정
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\config\JwtAuthenticationFilterTest.java::validTokenSetsAuthentication`, `missingTokenDoesNotSetAuthentication`
의존성: Task 28

## Task 30: SecurityConfig 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\config\SecurityConfig.java`
변경: 신규 생성. @Configuration @EnableWebSecurity. /admin/auth/login → permitAll, /api/v1/** → permitAll, /admin/** → authenticated, 나머지 → denyAll. CSRF disable, SESSION STATELESS
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\config\SecurityConfigTest.java::publicEndpointReturns200WithoutToken`, `adminEndpointReturns401WithoutToken`, `loginEndpointReturns200WithoutToken`
의존성: Task 29

## Task 31: application.yml 어드민 자격증명 설정 추가
파일: `D:\study\sports_site\backend\src\main\resources\application.yml`
변경: 기존 파일 수정. admin.username: ${ADMIN_USERNAME}, admin.password: ${ADMIN_PASSWORD} 추가
테스트: 없음
의존성: Task 30

## Task 32: .env.example 어드민 자격증명 추가
파일: `D:\study\sports_site\.env.example`
변경: 기존 파일 수정. ADMIN_USERNAME=, ADMIN_PASSWORD= 항목 추가
테스트: 없음
의존성: Task 31

## Task 33: AdminAuthController 생성 — POST /admin/auth/login
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\auth\AdminAuthController.java`
변경: 신규 생성. @RestController @RequestMapping("/admin/auth"). login(@Valid @RequestBody LoginRequest) → 자격증명 비교 → JwtTokenProvider.generateToken() 호출 → ApiResponse.ok(token). 불일치 시 BusinessException
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\auth\AdminAuthControllerTest.java::loginWithValidCredentialsReturnsToken`, `loginWithInvalidCredentialsReturns401`
의존성: Task 28, Task 30, Task 6

## Task 34: LoginRequest DTO 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\auth\LoginRequest.java`
변경: 신규 생성. record LoginRequest(@NotBlank String username, @NotBlank String password)
테스트: 없음
의존성: Task 3

---

## SECTION D — 공개 REST API (1-4)

## Task 35: GameService 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\game\GameService.java`
변경: 신규 생성. @Service. List<Game> findAll()
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\game\GameServiceTest.java::findAllReturnsAllGames`
의존성: Task 21

## Task 36: GameController 생성 — GET /api/v1/games
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\game\GameController.java`
변경: 신규 생성. @RestController @RequestMapping("/api/v1/games"). list() → ApiResponse.ok(gameService.findAll())
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\game\GameControllerTest.java::listReturns200WithGames`
의존성: Task 35, Task 4

## Task 37: MatchQueryService 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\match\MatchQueryService.java`
변경: 신규 생성. @Service. findMatches(status, gameId, date, pageable), findById(id), findUpcoming(), findResults()
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\match\MatchQueryServiceTest.java::findUpcomingReturnsScheduledMatches`, `findByIdThrowsWhenNotFound`
의존성: Task 24, Task 25

## Task 38: MatchController 생성 — 공개 엔드포인트 4개
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\match\MatchController.java`
변경: 신규 생성. @RestController @RequestMapping("/api/v1/matches"). list(), getById(), upcoming(), results()
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\match\MatchControllerTest.java::listReturns200`, `getByIdReturns404WhenNotFound`, `upcomingReturns200`, `resultsReturns200`
의존성: Task 37, Task 4

## Task 39: TeamQueryService 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\team\TeamQueryService.java`
변경: 신규 생성. @Service. findAll(gameId), findById(id) — 없으면 BusinessException
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\team\TeamQueryServiceTest.java::findAllByGameIdFiltersCorrectly`, `findByIdThrowsWhenNotFound`
의존성: Task 22, Task 6

## Task 40: TeamController 생성 — 공개 엔드포인트 2개
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\team\TeamController.java`
변경: 신규 생성. @RestController @RequestMapping("/api/v1/teams"). list(gameId), getById(id) — 소속 선수 포함 반환
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\team\TeamControllerTest.java::listReturns200`, `getByIdReturns200WithPlayers`, `getByIdReturns404WhenNotFound`
의존성: Task 39, Task 23, Task 4

## Task 41: PlayerQueryService 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\player\PlayerQueryService.java`
변경: 신규 생성. @Service. findById(id) — 없으면 BusinessException
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\player\PlayerQueryServiceTest.java::findByIdThrowsWhenNotFound`
의존성: Task 23, Task 6

## Task 42: PlayerController 생성 — 공개 엔드포인트 1개
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\player\PlayerController.java`
변경: 신규 생성. @RestController @RequestMapping("/api/v1/players"). getById(id)
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\player\PlayerControllerTest.java::getByIdReturns200`, `getByIdReturns404WhenNotFound`
의존성: Task 41, Task 4

---

## SECTION E — 어드민 REST API (1-4)

## Task 43: MatchCommandService 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\match\MatchCommandService.java`
변경: 신규 생성. @Service @Transactional. create(MatchCreateRequest), update(id, MatchUpdateRequest), delete(id)
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\match\MatchCommandServiceTest.java::createPersistsMatch`, `updateChangesFields`, `deleteRemovesMatch`, `deleteThrowsWhenNotFound`
의존성: Task 24, Task 6

## Task 44: MatchCreateRequest DTO 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\match\MatchCreateRequest.java`
변경: 신규 생성. record MatchCreateRequest(@NotNull Long gameId, @NotNull Long teamAId, @NotNull Long teamBId, @NotBlank String tournamentName, String stage, @NotNull @Future LocalDateTime scheduledAt)
테스트: 없음
의존성: Task 3

## Task 45: MatchUpdateRequest DTO 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\match\MatchUpdateRequest.java`
변경: 신규 생성. record MatchUpdateRequest(String tournamentName, String stage, LocalDateTime scheduledAt, MatchStatus status)
테스트: 없음
의존성: Task 18

## Task 46: AdminMatchController 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\match\AdminMatchController.java`
변경: 신규 생성. @RestController @RequestMapping("/admin/matches"). create, update, delete
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\match\AdminMatchControllerTest.java::createReturns201`, `createWithoutAuthReturns401`, `updateReturns200`, `deleteReturns204`
의존성: Task 43, Task 44, Task 45, Task 30

## Task 47: MatchResultCommandService 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\matchresult\MatchResultCommandService.java`
변경: 신규 생성. @Service @Transactional. create(matchId, request) — 중복 시 BusinessException, update(matchId, request)
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\matchresult\MatchResultCommandServiceTest.java::createPersistsResult`, `createThrowsOnDuplicateMatchId`, `updateChangesScore`
의존성: Task 25, Task 24, Task 6

## Task 48: MatchResultRequest DTO 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\matchresult\MatchResultRequest.java`
변경: 신규 생성. record MatchResultRequest(@NotNull Long winnerTeamId, @Min(0) int scoreTeamA, @Min(0) int scoreTeamB, @NotNull @PastOrPresent LocalDateTime playedAt, String vodUrl, String notes)
테스트: 없음
의존성: Task 3

## Task 49: AdminMatchResultController 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\matchresult\AdminMatchResultController.java`
변경: 신규 생성. @RestController @RequestMapping("/admin/matches/{matchId}/result"). create, update
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\matchresult\AdminMatchResultControllerTest.java::createReturns201`, `createWithoutAuthReturns401`, `createDuplicateReturns409`
의존성: Task 47, Task 48, Task 30

## Task 50: TeamCommandService 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\team\TeamCommandService.java`
변경: 신규 생성. @Service @Transactional. create(TeamRequest), update(id, TeamRequest), delete(id)
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\team\TeamCommandServiceTest.java::createPersistsTeam`, `updateChangesName`, `deleteRemovesTeam`
의존성: Task 22, Task 6

## Task 51: TeamRequest DTO 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\team\TeamRequest.java`
변경: 신규 생성. record TeamRequest(@NotBlank String name, String shortName, String region, String logoUrl, String externalId, @NotNull Long gameId)
테스트: 없음
의존성: Task 3

## Task 52: AdminTeamController 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\team\AdminTeamController.java`
변경: 신규 생성. @RestController @RequestMapping("/admin/teams"). create, update, delete
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\team\AdminTeamControllerTest.java::createReturns201`, `createWithoutAuthReturns401`, `deleteReturns204`
의존성: Task 50, Task 51, Task 30

## Task 53: PlayerCommandService 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\player\PlayerCommandService.java`
변경: 신규 생성. @Service @Transactional. create(PlayerRequest), update(id, PlayerRequest), delete(id)
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\player\PlayerCommandServiceTest.java::createPersistsPlayer`, `updateChangesRole`, `deleteRemovesPlayer`
의존성: Task 23, Task 6

## Task 54: PlayerRequest DTO 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\player\PlayerRequest.java`
변경: 신규 생성. record PlayerRequest(@NotBlank String inGameName, String realName, String role, String nationality, String profileImageUrl, Long teamId, String externalId)
테스트: 없음
의존성: Task 3

## Task 55: AdminPlayerController 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\domain\player\AdminPlayerController.java`
변경: 신규 생성. @RestController @RequestMapping("/admin/players"). create, update, delete
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\domain\player\AdminPlayerControllerTest.java::createReturns201`, `createWithoutAuthReturns401`, `deleteReturns204`
의존성: Task 53, Task 54, Task 30

---

## SECTION F — PandaScore 연동 (1-3)

## Task 56: PandaScoreProperties 설정 클래스 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\config\PandaScoreProperties.java`
변경: 신규 생성. @ConfigurationProperties(prefix="pandascore"). String apiKey (→ ${PANDASCORE_API_KEY}), String baseUrl. 하드코딩 없음
테스트: 없음
의존성: Task 3

## Task 57: application.yml PandaScore 설정 추가
파일: `D:\study\sports_site\backend\src\main\resources\application.yml`
변경: 기존 파일 수정. pandascore.api-key: ${PANDASCORE_API_KEY}, pandascore.base-url: https://api.pandascore.co 추가
테스트: 없음
의존성: Task 56

## Task 58: ExternalMatchDto 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\external\ExternalMatchDto.java`
변경: 신규 생성. record ExternalMatchDto(@NotBlank String externalId, @NotBlank String tournamentName, @NotNull LocalDateTime scheduledAt, @NotBlank String teamAExternalId, @NotBlank String teamBExternalId, String gameSlug, String stage). 필드 누락 시 REJECT — 추정 보간 금지 (Hard Rule #8)
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\external\ExternalMatchDtoTest.java::validationFailsWhenExternalIdBlank`, `validationFailsWhenScheduledAtNull`
의존성: Task 3

## Task 59: PandaScoreClient 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\external\PandaScoreClient.java`
변경: 신규 생성. @Component. fetchUpcomingMatches() — GET /matches/upcoming, Authorization: Bearer ${PANDASCORE_API_KEY}. HTTP 오류 시 BusinessException. API 키는 PandaScoreProperties에서만 주입
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\external\PandaScoreClientTest.java::fetchUpcomingMatchesReturnsData`, `apiErrorThrowsBusinessException`
의존성: Task 56, Task 57, Task 58

## Task 60: ExternalSyncService 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\external\ExternalSyncService.java`
변경: 신규 생성. @Service @Transactional. syncMatches() — fetchUpcomingMatches() → 각 항목 @Valid 검증 → 실패 항목 REJECT (저장 없음) → externalId 기준 upsert → SyncResult { int synced, int rejected } 반환
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\external\ExternalSyncServiceTest.java::syncMatchesPersistsValidItems`, `invalidItemsAreRejectedNotPersisted`, `existingExternalIdUpdatesNotDuplicates`
의존성: Task 59, Task 24, Task 22, Task 21

## Task 61: SyncResult DTO 생성
파일: `D:\study\sports_site\backend\src\main\java\com\esports\external\SyncResult.java`
변경: 신규 생성. record SyncResult(int synced, int rejected)
테스트: 없음
의존성: Task 3

## Task 62: AdminSyncController 생성 — POST /admin/external/sync
파일: `D:\study\sports_site\backend\src\main\java\com\esports\external\AdminSyncController.java`
변경: 신규 생성. @RestController @RequestMapping("/admin/external"). sync() → ExternalSyncService.syncMatches() → ApiResponse.ok(syncResult)
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\external\AdminSyncControllerTest.java::syncReturns200WithResult`, `syncWithoutAuthReturns401`
의존성: Task 60, Task 61, Task 30, Task 4

---

## SECTION G — 프론트엔드 기본 구조 (1-1 연속)

## Task 63: 프론트엔드 도메인 타입 파일 생성
파일: `D:\study\sports_site\frontend\src\types\domain.ts`
변경: 신규 생성. 인터페이스 Game, Team, Player, Match, MatchResult TypeScript 타입 정의
테스트: 없음
의존성: Task 7

## Task 64: 경기 API 호출 함수 생성
파일: `D:\study\sports_site\frontend\src\api\matches.ts`
변경: 신규 생성. getMatches(), getMatchById(), getUpcomingMatches(), getMatchResults()
테스트: 없음
의존성: Task 10, Task 63

## Task 65: 팀 API 호출 함수 생성
파일: `D:\study\sports_site\frontend\src\api\teams.ts`
변경: 신규 생성. getTeams(gameId?), getTeamById(id)
테스트: 없음
의존성: Task 10, Task 63

## Task 66: 게임 API 호출 함수 생성
파일: `D:\study\sports_site\frontend\src\api\games.ts`
변경: 신규 생성. getGames()
테스트: 없음
의존성: Task 10, Task 63

## Task 67: React 앱 진입점 생성
파일: `D:\study\sports_site\frontend\src\main.tsx`
변경: 신규 생성. ReactDOM.createRoot. QueryClientProvider + BrowserRouter 래핑
테스트: 없음
의존성: Task 7

## Task 68: 기본 라우팅 설정
파일: `D:\study\sports_site\frontend\src\App.tsx`
변경: 신규 생성. Routes: / → MatchSchedulePage, /results → MatchResultsPage, /teams → TeamsPage, /teams/:id → TeamDetailPage, /players/:id → PlayerDetailPage. 페이지 컴포넌트는 placeholder (UI는 Phase 2)
테스트: 없음
의존성: Task 67

---

## SECTION H — 테스트 스위트 (1-5)

## Task 69: TestcontainersConfig 생성
파일: `D:\study\sports_site\backend\src\test\java\com\esports\TestcontainersConfig.java`
변경: 신규 생성. @TestConfiguration. PostgreSQLContainer 선언. @DynamicPropertySource로 datasource 동적 주입
테스트: 없음 (테스트 인프라)
의존성: Task 2

## Task 70: 통합 테스트 — Flyway 마이그레이션 검증
파일: `D:\study\sports_site\backend\src\test\java\com\esports\FlywayMigrationTest.java`
변경: 신규 생성. migrationsApplySuccessfully() — 모든 마이그레이션 SUCCESS 확인. tablesExist() — 5개 테이블 존재 확인
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\FlywayMigrationTest.java::migrationsApplySuccessfully`, `tablesExist`
의존성: Task 14, Task 69

## Task 71: 통합 테스트 — 공개 API 접근성
파일: `D:\study\sports_site\backend\src\test\java\com\esports\PublicApiIntegrationTest.java`
변경: 신규 생성. 토큰 없이 /api/v1/** 엔드포인트 200 응답 확인
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\PublicApiIntegrationTest.java::gamesEndpointReturns200WithoutToken`, `matchesEndpointReturns200WithoutToken`
의존성: Task 36, Task 38, Task 30, Task 69

## Task 72: 통합 테스트 — 어드민 API 인증 강제
파일: `D:\study\sports_site\backend\src\test\java\com\esports\AdminApiAuthIntegrationTest.java`
변경: 신규 생성. 토큰 없이 /admin/** 엔드포인트 401 응답 확인. 유효 자격증명으로 로그인 시 토큰 반환 확인
테스트: `D:\study\sports_site\backend\src\test\java\com\esports\AdminApiAuthIntegrationTest.java::adminMatchesWithoutTokenReturns401`, `loginWithValidCredentialsReturnsToken`
의존성: Task 30, Task 33, Task 46, Task 52, Task 55, Task 62, Task 69

---

## 태스크 의존성 요약

| 단계 | 태스크 범위 | 선행 조건 |
|------|------------|---------|
| 프로젝트 초기화 | Task 1–12 | 없음 |
| DB 엔티티/레포지토리 | Task 13–25 | Task 1–12 완료 |
| JWT 인프라 | Task 26–34 | Task 1–12 완료 |
| 공개 API | Task 35–42 | Task 13–25, 26–34 완료 |
| 어드민 API | Task 43–55 | Task 13–25, 26–34 완료 |
| PandaScore 연동 | Task 56–62 | Task 13–25, 26–34 완료 |
| 프론트엔드 | Task 63–68 | Task 7–10 완료 |
| 테스트 스위트 | Task 69–72 | 앞선 모든 태스크 완료 |

**총 태스크 수: 72개**
