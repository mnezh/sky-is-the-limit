IMAGE_NAME = my-testing-framework
TAG = latest
REPORT_DIR = $(shell pwd)/cucumber-reports
# NEW: Define the path inside the container where reports will be mounted
CONTAINER_REPORT_PATH = /reports

.PHONY: build run run-with-args clean

# Target to build the Docker image
build:
	@echo "--- Building Docker image: $(IMAGE_NAME):$(TAG) ---"
	docker build -t $(IMAGE_NAME):$(TAG) .
	@echo "--- Build complete. Image: $(IMAGE_NAME):$(TAG) ---"

# Target to run all tests with a mounted volume for reports
run:
	@mkdir -p $(REPORT_DIR) || true # Ensure the directory exists
	@echo "--- Running all tests in Docker. Reports will be saved to: $(REPORT_DIR) ---"
	docker run --rm \
		-v $(REPORT_DIR):$(CONTAINER_REPORT_PATH) \
		$(IMAGE_NAME):$(TAG) \
		-Preport.path=$(CONTAINER_REPORT_PATH)

# Target to run tests with specific Cucumber tags and environment properties
# Arguments passed as environment variables (TAGS, BASE_URL, etc.) are converted to Gradle -P flags.
# Example usage: make run-with-args TAGS="@auth" BASE_URL="http://dev-api.com"
run-with-args:
	@mkdir -p $(REPORT_DIR) || true
	@echo "--- Running tests in Docker with arguments. Reports will be saved to: $(REPORT_DIR) ---"
	# Construct Gradle arguments from environment variables
	$(eval GRADLE_ARGS := \
	-Preport.path=$(CONTAINER_REPORT_PATH) \
	$(if $(TAGS), -Ptags=$(TAGS),) \
	$(if $(BASE_URL), -Pbase.url=$(BASE_URL),) \
	$(if $(USERNAME), -Pusername=$(USERNAME),) \
	$(if $(PASSWORD), -Ppassword=$(PASSWORD),))

	docker run --rm \
		-v $(REPORT_DIR):$(CONTAINER_REPORT_PATH) \
		$(IMAGE_NAME):$(TAG) \
		$(GRADLE_ARGS)

# Clean up the local reports directory
clean:
	@echo "--- Cleaning local reports directory: $(REPORT_DIR) ---"
	rm -rf $(REPORT_DIR)
	@echo "--- Clean complete ---"
