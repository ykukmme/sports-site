# === 빌드 스테이지 ===
# Gradle 빌드용 JDK 17 이미지 (테스트 제외 — DB 없는 환경에서 빌드)
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# 의존성 캐시 최적화 — build.gradle 변경 시에만 재다운로드
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
# gradlew 실행 권한 부여 (Windows에서 클론 시 권한 비트 유실 방지)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# 소스 복사 후 빌드 (테스트 제외 — 통합 테스트는 CI에서 별도 실행)
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test -q

# === 실행 스테이지 ===
# JRE만 포함한 경량 이미지 (보안: JDK 불포함)
FROM eclipse-temurin:17-jre-alpine AS runtime

# 보안: root 대신 전용 사용자로 실행
RUN addgroup -S esports && adduser -S esports -G esports
USER esports

WORKDIR /app

# 빌드 결과물만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring Boot Actuator 헬스체크 — Docker/AWS 로드밸런서가 이 엔드포인트로 상태 확인
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
