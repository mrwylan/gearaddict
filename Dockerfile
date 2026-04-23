# Multi-arch OCI image for GearAddict — see docs/adr/ADR-008-container-image.md
#
# Build inputs:
#   - A Spring Boot layered jar at target/*.jar, produced by `./mvnw -Pproduction package`
# Build command (CI):
#   docker buildx build --platform linux/amd64,linux/arm64 --push -t <image>:<tag> .
# Build command (local, single-arch):
#   docker buildx build --load -t gearaddict:local .

# Extract layers on the BUILDPLATFORM — extracted content is JVM-bytecode / JAR resources,
# so the same output is valid for every target architecture. Running this stage natively
# (not under QEMU) keeps multi-arch builds fast.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jre AS extractor
WORKDIR /builder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher

# Runtime stage — pulled per TARGETPLATFORM by buildx, producing one layer per arch
# that is then stitched into a single multi-arch manifest list on push.
FROM eclipse-temurin:25-jre
WORKDIR /application
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=extractor --chown=spring:spring /builder/application/dependencies/ ./
COPY --from=extractor --chown=spring:spring /builder/application/spring-boot-loader/ ./
COPY --from=extractor --chown=spring:spring /builder/application/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /builder/application/application/ ./
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
