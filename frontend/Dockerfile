# Web image (RNF-06, RNF-16): the Angular production build inside Caddy, with deploy/Caddyfile.
# The context is the repository root: docker build -f frontend/Dockerfile .
FROM node:26-alpine AS build
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY frontend/ ./
RUN npm run build

FROM caddy:2.11-alpine
ARG REVISION=unknown
LABEL org.opencontainers.image.source="https://github.com/xosebabio/buddy-fantasy" \
      org.opencontainers.image.revision="${REVISION}"
COPY deploy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /build/frontend/dist/buddy-fantasy/browser /srv
