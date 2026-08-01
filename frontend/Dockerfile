FROM node:20 AS build

WORKDIR /app

COPY package.json ./
RUN npm install

COPY . .
ARG CONFIGURATION=production
RUN npm run build -- --configuration=${CONFIGURATION}

FROM nginx:alpine

COPY --from=build /app/dist/hr-ai-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
