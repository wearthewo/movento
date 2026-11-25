# Next.js standalone image. Build context is the repository root (for .local-certs/).
FROM node:22-alpine AS base
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
# Bundle any optional HTTPS-interception CAs into one file that Node trusts (no-op when none exist).
COPY .local-certs/ /opt/movento-local-certs/
RUN cat /opt/movento-local-certs/*.pem > /opt/movento-local-certs/bundle.crt 2>/dev/null || touch /opt/movento-local-certs/bundle.crt
ENV NODE_EXTRA_CA_CERTS=/opt/movento-local-certs/bundle.crt

FROM base AS dependencies
COPY web/package.json web/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

FROM dependencies AS build
COPY web/ ./
ARG GATEWAY_URL=http://api-gateway:8080
ARG NEXT_PUBLIC_APP_URL=http://localhost:3000
ARG SKIP_CHECKS=false
ENV GATEWAY_URL=$GATEWAY_URL
ENV NEXT_PUBLIC_APP_URL=$NEXT_PUBLIC_APP_URL
# CI already lints/typechecks; pass --build-arg SKIP_CHECKS=true for faster image builds there.
RUN if [ "$SKIP_CHECKS" != "true" ]; then npm run lint && npm run typecheck; fi && npm run build

FROM base AS runtime
ENV NODE_ENV=production \
    HOSTNAME=0.0.0.0 \
    PORT=3000
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
COPY --from=build --chown=node:node /app/public ./public
# Optimized-image cache; mounted as a volume in docker-compose so it survives rebuilds.
RUN mkdir -p .next/cache && chown -R node:node .next/cache
USER node
EXPOSE 3000
HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
  CMD node -e "fetch('http://127.0.0.1:3000/api/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node", "server.js"]
