# Một image chứa cả 7 service Java. Mỗi container chọn jar bằng `command`
# trong docker-compose.yml (ENTRYPOINT luôn là `java -jar`).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app \
    && apk add --no-cache tzdata
COPY --from=build /src/api-gateway/target/api-gateway-*.jar /app/api-gateway.jar
COPY --from=build /src/user-service/target/user-service-*.jar /app/user-service.jar
COPY --from=build /src/movie-service/target/movie-service-*.jar /app/movie-service.jar
COPY --from=build /src/venue-service/target/venue-service-*.jar /app/venue-service.jar
COPY --from=build /src/booking-service/target/booking-service-*.jar /app/booking-service.jar
COPY --from=build /src/payment-service/target/payment-service-*.jar /app/payment-service.jar
COPY --from=build /src/notification-service/target/notification-service-*.jar /app/notification-service.jar
ENV TZ=Asia/Ho_Chi_Minh
USER app
ENTRYPOINT ["java", "-jar"]
