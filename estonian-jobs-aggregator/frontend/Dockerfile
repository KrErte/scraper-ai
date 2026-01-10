# Multi-stage build Angular rakenduse jaoks

# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Kopeeri package failid ja installi sõltuvused
COPY package*.json ./
RUN npm ci

# Kopeeri lähtekood ja ehita
COPY . .
RUN npm run build:prod

# Runtime stage
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Eemalda vaikimisi nginx sisu
RUN rm -rf ./*

# Kopeeri ehitatud rakendus
COPY --from=build /app/dist/estonian-jobs-frontend/browser .

# Kopeeri nginx konfiguratsioon
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Ava port
EXPOSE 80

# Tervisekontroll
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/health || exit 1

# Käivita nginx
CMD ["nginx", "-g", "daemon off;"]
