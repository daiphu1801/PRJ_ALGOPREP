FROM node:22-bookworm-slim
WORKDIR /app
ENV NODE_ENV=production HOSTNAME=0.0.0.0 PORT=3000 NEXT_TELEMETRY_DISABLED=1
ADD --chown=node:node frontend-standalone.tgz ./
USER node
EXPOSE 3000
CMD ["node", "frontend/server.js"]
