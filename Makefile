# ============================================================
# Makefile - Development Helper Commands
# ============================================================
# 
# Usage: make [command]
# 
# Quick start:
#   make help        -> List all commands
#   make up          -> Start docker-compose
#   make logs        -> View logs
#   make db          -> Access PostgreSQL CLI
#

.PHONY: help up down build rebuild logs db bash clean test

# Default target
help:
	@echo "╔════════════════════════════════════════════════════════╗"
	@echo "║        Veritas - Development Commands                  ║"
	@echo "╚════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "🚀 Container Management:"
	@echo "  make up              Start all containers (-d background)"
	@echo "  make down            Stop all containers"
	@echo "  make restart         Restart all containers"
	@echo "  make build           Build Docker image (no cache)"
	@echo "  make rebuild         Rebuild and start from scratch"
	@echo ""
	@echo "📊 Monitoring:"
	@echo "  make logs            Show all logs (-f follow)"
	@echo "  make logs-app        Show Spring Boot logs"
	@echo "  make logs-db         Show PostgreSQL logs"
	@echo "  make ps              Show container status"
	@echo ""
	@echo "🗄️  Database:"
	@echo "  make db              Open PostgreSQL CLI (psql)"
	@echo "  make db-reset        Drop and recreate database"
	@echo ""
	@echo "💻 Development:"
	@echo "  make bash            Access Spring Boot container shell"
	@echo "  make bash-db         Access PostgreSQL container shell"
	@echo ""
	@echo "🧹 Cleanup:"
	@echo "  make clean           Remove containers (keep volumes)"
	@echo "  make clean-all       Remove everything (containers + volumes)"
	@echo ""
	@echo "📝 Setup:"
	@echo "  make env             Create .env from .env.example"
	@echo ""

# ====== Container Commands ======

up:
	@echo "🚀 Starting containers..."
	docker-compose up -d
	@echo "✅ Done! Access at http://localhost:8080"
	@echo "   DB at localhost:5432 (dev_user / dev_password)"

down:
	@echo "🛑 Stopping containers..."
	docker-compose down

restart:
	@echo "🔄 Restarting containers..."
	docker-compose restart

build:
	@echo "🔨 Building Docker image (no cache)..."
	docker-compose build --no-cache

rebuild: clean-all up
	@echo "✅ Rebuilt from scratch!"

# ====== Logging Commands ======

logs:
	docker-compose logs -f

logs-app:
	docker-compose logs -f veritas-app

logs-db:
	docker-compose logs -f postgres-local

ps:
	docker-compose ps

# ====== Database Commands ======

db:
	@echo "📊 Connecting to PostgreSQL..."
	docker-compose exec postgres-local psql -U dev_user -d veritas_dev

db-reset:
	@echo "⚠️  Resetting database..."
	docker-compose down -v
	docker-compose up -d postgres-local
	@echo "✅ Database reset complete!"

# ====== Shell Access ======

bash:
	docker-compose exec veritas-app bash

bash-db:
	docker-compose exec postgres-local bash

# ====== Cleanup ======

clean:
	@echo "🧹 Removing containers (keeping volumes)..."
	docker-compose down

clean-all:
	@echo "🧹 Removing EVERYTHING (containers + volumes)..."
	docker-compose down -v
	@echo "✅ Clean complete!"

# ====== Setup ======

env:
	@if [ ! -f .env ]; then \
		echo "📝 Creating .env from .env.example..."; \
		cp .env.example .env; \
		echo "✅ Created .env - adjust settings if needed"; \
	else \
		echo "⚠️  .env already exists"; \
	fi

# ====== Utility ======

status:
	@echo "📊 Current Status:"
	@docker-compose ps
	@echo ""
	@echo "📋 Docker Images:"
	@docker images | grep veritas

# ====== Development Workflow ======

dev: env up logs-app
	@echo "🚀 Development environment ready!"

fresh: clean-all env up
	@echo "✅ Fresh environment started!"
