FROM eclipse-temurin:21-jdk-jammy
VOLUME /tmp
COPY build/libs/order-service-1.3.jar OrderService.jar
ENTRYPOINT ["java", "-jar", "OrderService.jar"]