DOCKER_COMPOSE_CMD ?= sudo docker-compose

.PHONY: build
build:
	$(DOCKER_COMPOSE_CMD) build

.PHONY: start 
start:
	$(DOCKER_COMPOSE_CMD) up --force-recreate --remove-orphans --detach
	@echo ""
	@echo "Otel demo is running."
	@echo "<TODO> list available service and ports"

.PHONY: stop
stop:	$(DOCKER_COMPOSE_CMD) down --remove-orphans --volumes
	@echo ""
	@echo "Otel demo is stopped"

# Use to restart a single service component
# Example: make restart service=frontend
.PHONY: restart
restart:
# work with `service` or `SERVICE` as input
ifdef SERVICE
	service := $(SERVICE)
endif

ifdef service
	$(DOCKER_COMPOSE_CMD) stop $(service)
	$(DOCKER_COMPOSE_CMD) rm --force $(service)
	$(DOCKER_COMPOSE_CMD) create $(service)
	$(DOCKER_COMPOSE_CMD) start $(service)
else
	@echo "Please provide a service name using `service=[service name]` or `SERVICE=[service name]`"
endif

# Use to rebuild and restart (redeploy) a single service component
# Example: make redeploy service=frontend
.PHONY: redeploy
redeploy:
# work with `service` or `SERVICE` as input
ifdef SERVICE
	service := $(SERVICE)
endif

ifdef service
	$(DOCKER_COMPOSE_CMD) build $(service)
	$(DOCKER_COMPOSE_CMD) stop $(service)
	$(DOCKER_COMPOSE_CMD) rm --force $(service)
	$(DOCKER_COMPOSE_CMD) create $(service)
	$(DOCKER_COMPOSE_CMD) start $(service)
else
	@echo "Please provide a service name using `service=[service name]` or `SERVICE=[service name]`"
endif
