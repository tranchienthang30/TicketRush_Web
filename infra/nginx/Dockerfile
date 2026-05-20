FROM node:22-alpine AS frontend-build

WORKDIR /app

COPY ticketFrontend/package*.json ./
RUN npm ci

COPY ticketFrontend/ ./

ARG VITE_API_BASE_URL=
ARG VITE_RECAPTCHA_SITE_KEY=
ARG VITE_QR_PUBLIC_BASE_URL=
ARG VITE_SEATSIO_REGION=eu

ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_RECAPTCHA_SITE_KEY=$VITE_RECAPTCHA_SITE_KEY
ENV VITE_QR_PUBLIC_BASE_URL=$VITE_QR_PUBLIC_BASE_URL
ENV VITE_SEATSIO_REGION=$VITE_SEATSIO_REGION

RUN npm run build

FROM nginx:1.27-alpine

COPY infra/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend-build /app/dist /usr/share/nginx/html

EXPOSE 80
