# 1단계: React 앱 빌드
FROM node:18 AS build
WORKDIR /app
COPY . .
RUN npm install && npm run build

# 2단계: Nginx로 정적 파일 서빙
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80 