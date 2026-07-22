FROM node:22-alpine AS base
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
COPY .local-certs/ /opt/movento-local-certs/
ENV NODE_EXTRA_CA_CERTS=/opt/movento-local-certs/avast-web-mail-shield-root.pem

FROM base AS dependencies
COPY web/package.json web/package-lock.json ./
RUN npm ci --no-audit --no-fund

FROM dependencies AS build
COPY web/ ./
ARG GATEWAY_URL=http://api-gateway:8080
ARG NEXT_PUBLIC_APP_URL=http://localhost:3000
ENV GATEWAY_URL=$GATEWAY_URL
ENV NEXT_PUBLIC_APP_URL=$NEXT_PUBLIC_APP_URL
RUN npm run lint && npm run typecheck && npm run build

FROM base AS runtime
ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
USER node
EXPOSE 3000
CMD ["node", "server.js"]
