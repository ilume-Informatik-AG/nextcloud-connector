FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*-exec.jar app.jar
ENTRYPOINT ["java", "-Dloader.path=/opt/app", "-cp", "app.jar", "org.springframework.boot.loader.launch.PropertiesLauncher"]
