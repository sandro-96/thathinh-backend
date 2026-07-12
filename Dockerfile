FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src ./src
RUN chmod +x mvnw && ./mvnw -DskipTests package -q

# Jammy (Debian) — tránh lỗi SSL MongoDB Atlas trên Alpine (fatal alert: internal_error).
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/thathinh-backend-0.0.1-SNAPSHOT.jar app.jar

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=70.0", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
