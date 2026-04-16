# === 빌드 스테이지 ===
FROM node:20-alpine AS builder

WORKDIR /app

# 의존성 캐시 최적화 — package.json/lock 변경 시에만 재설치
# npm ci: package-lock.json 기반 정확한 버전 설치 (재현성 보장)
COPY package.json package-lock.json ./
RUN npm ci --silent

# 소스 복사 후 빌드
COPY . .
RUN npm run build

# === 실행 스테이지 ===
# nginx-unprivileged: root 없이 실행되는 nginx 이미지 (보안 강화)
# 기본 포트 80으로 리슨 (내부 포트, docker-compose에서 외부 포트 매핑)
FROM nginxinc/nginx-unprivileged:alpine AS runtime

# envsubst 설치 — 런타임에 BACKEND_INTERNAL_URL 등 환경변수를 nginx 설정에 주입
USER root
RUN apk add --no-cache gettext
USER nginx

# nginx 설정 템플릿 복사 — 컨테이너 시작 시 envsubst로 실제 설정 파일 생성
COPY --chown=nginx:nginx nginx.conf.template /etc/nginx/conf.d/default.conf.template

# 빌드 결과물 복사
COPY --from=builder --chown=nginx:nginx /app/dist /usr/share/nginx/html

EXPOSE 80

# 시작 시 BACKEND_INTERNAL_URL 환경변수를 nginx 설정에 주입 후 nginx 실행
# nginx 내장 변수($host 등)와 충돌 방지를 위해 치환 대상 변수를 명시적으로 지정
CMD ["/bin/sh", "-c", "envsubst '${BACKEND_INTERNAL_URL}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g 'daemon off;'"]
