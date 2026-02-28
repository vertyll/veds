SERVICES = api-gateway identity-service mail-service
SHARED = shared-infrastructure

.PHONY: build-all test-all clean-all format-all check-style-all

build-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Building $$dir..."; \
		(cd $$dir && ./gradlew build) || exit 1; \
	done

test-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Testing $$dir..."; \
		(cd $$dir && ./gradlew test) || exit 1; \
	done

clean-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Cleaning $$dir..."; \
		(cd $$dir && ./gradlew clean) || exit 1; \
	done

format-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Formatting $$dir..."; \
		(cd $$dir && ./gradlew ktlintFormat) || exit 1; \
	done

check-style-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Checking style $$dir..."; \
		(cd $$dir && ./gradlew ktlintCheck detekt) || exit 1; \
	done
