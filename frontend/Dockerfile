# Multi-stage build for the React frontend
FROM node:20-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Unprivileged nginx: runs as a non-root user and listens on 8080, so it works on OpenShift too.
FROM nginxinc/nginx-unprivileged:alpine

# The nginx image runs envsubst over /etc/nginx/templates/*.template at start-up.
# Only BACKEND_HOST is substituted, so nginx's own $variables are left alone.
ENV BACKEND_HOST=backend \
    NGINX_ENVSUBST_FILTER=BACKEND_HOST

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template

# OpenShift assigns an arbitrary UID in group 0. envsubst must be able to write the generated config.
USER root
RUN chgrp -R 0 /etc/nginx/conf.d && chmod -R g=u /etc/nginx/conf.d
USER 101:0

EXPOSE 8080
