# Stage 1: Builder - Used to download dependencies and compile code
# Updated to use gradle:9.2-jdk25-alpine as requested
FROM gradle:9.2-jdk25-alpine AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Gradle configuration files first
# This allows Docker to cache the dependencies layer if only build/config files change
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY gradle.properties .

# Download dependencies
# A dummy run of 'build' or 'test' is often used to ensure all dependencies are resolved and cached.
# Using --dry-run to avoid actual execution, but still hit the dependency resolution logic.
RUN ./gradlew clean build --console=plain --warning-mode all --refresh-dependencies --dry-run || true

# Copy all the project source code
COPY . .


# --- Stage 2: Final Runner Image ---

# FIX: Changed from JRE to JDK (Java Development Kit) to ensure the Java Compiler (javac) is present,
# which is needed for the Gradle 'test' task to execute the :compileTestJava step.
FROM eclipse-temurin:25-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the Gradle user home (dependency cache) and the project source from the builder stage
# IMPORTANT: Copying to /root/.gradle ensures Gradle can find the cache when running
# as the default root user in the Alpine JDK image.
COPY --from=builder /home/gradle/.gradle /root/.gradle

# Copy the application source and gradle wrapper/configs
COPY --from=builder /app /app

# Make the gradle wrapper executable (standard practice for entrypoint)
RUN chmod +x gradlew

# NEW: The volume is now mounted outside the /app/build directory to prevent the :clean task failure.
# Gradle will write reports to a custom location.
VOLUME /reports

# Set the default entry point to run the tests
# Arguments like -Ptags=... or -Pbase.url=... can be appended at runtime
# We will pass the new report location via a system property in the Makefile.
ENTRYPOINT ["/app/gradlew", "--no-daemon", "clean", "test"]
