FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev

FROM node:20-alpine
WORKDIR /app

RUN addgroup -S hmcts && adduser -S hmcts -G hmcts

COPY --from=build /app/node_modules ./node_modules
COPY package*.json ./
COPY src ./src

USER hmcts

EXPOSE 3000

CMD ["node", "src/app.js"]
