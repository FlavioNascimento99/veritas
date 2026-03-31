#!/bin/bash
# ============================================================
# run.sh - Load .env and start the application
# ============================================================
# Usage: ./run.sh [dev|prod]
# ============================================================

set -a  # Export all variables
source .env
set +a

# Profile selection
PROFILE=${1:-dev}
export SPRING_PROFILES_ACTIVE=$PROFILE

echo "🚀 Starting Veritas with profile: $PROFILE"
echo "📊 Database: $PGHOST:$PGPORT/$PGDATABASE"
echo ""

# Use global Maven if available, otherwise use Maven wrapper
if command -v mvn &> /dev/null; then
    mvn spring-boot:run
else
    ./mvnw spring-boot:run
fi
