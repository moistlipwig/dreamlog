# Etap 1: build Angulara
FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build:prod
# Uwaga: domyślnie Angular CLI buduje do dist/<nazwa-projektu>/
# Jeżeli chcesz mieć pewność ścieżki, sprawdź "outputPath" w angular.json

# Etap 2: serwowanie przez Nginx
FROM nginx:stable-alpine
COPY docker/nginx/nginx.conf /etc/nginx/nginx.conf
# Skopiuj ZAWARTOŚĆ katalogu dist/<nazwa-projektu> do katalogu www
# Jeśli nazwa projektu to "dreamlog-frontend":
COPY --from=build /app/dist/dreamlog-frontend/ /usr/share/nginx/html/
