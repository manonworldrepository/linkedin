FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle

COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew nativeCompile --no-daemon

FROM ubuntu:jammy-20240530

WORKDIR /app

COPY --from=builder /workspace/app/build/native/nativeCompile/linkedin .

EXPOSE 8080

ENTRYPOINT ["./linkedin"]