#!/usr/bin/env bash
# API test examples — run these after: ./mvnw spring-boot:run
# Base URL assumes default port. Override with: BASE=http://localhost:9090

BASE=http://localhost:8080

# ── CREATE ────────────────────────────────────────────────────────────────────

# Minimal — only title required; status/priority default to TODO/MEDIUM
curl -s -X POST "$BASE/api/tasks" \
  -H "Content-Type: application/json" \
  -d '{"title": "Buy groceries"}' | jq .

# Full request
curl -s -X POST "$BASE/api/tasks" \
  -H "Content-Type: application/json" \
  -d '{
    "title":       "Plan Q4 launch",
    "description": "Coordinate with marketing, eng, and design",
    "status":      "IN_PROGRESS",
    "priority":    "HIGH",
    "dueDate":     "2025-12-01"
  }' | jq .

# Validation failure — missing title → 400 with field errors
curl -s -X POST "$BASE/api/tasks" \
  -H "Content-Type: application/json" \
  -d '{"description": "No title provided"}' | jq .

# ── READ ──────────────────────────────────────────────────────────────────────

# List all
curl -s "$BASE/api/tasks" | jq .

# Filter by status
curl -s "$BASE/api/tasks?status=TODO" | jq .

# Filter by priority
curl -s "$BASE/api/tasks?priority=HIGH" | jq .

# Filter by both
curl -s "$BASE/api/tasks?status=IN_PROGRESS&priority=HIGH" | jq .

# Get by ID
curl -s "$BASE/api/tasks/1" | jq .

# Not found → 404
curl -s "$BASE/api/tasks/9999" | jq .

# ── UPDATE (PATCH) ────────────────────────────────────────────────────────────

# Update status only — other fields unchanged
curl -s -X PATCH "$BASE/api/tasks/1" \
  -H "Content-Type: application/json" \
  -d '{"status": "DONE"}' | jq .

# Update multiple fields
curl -s -X PATCH "$BASE/api/tasks/1" \
  -H "Content-Type: application/json" \
  -d '{
    "title":    "Buy groceries (updated)",
    "priority": "LOW",
    "dueDate":  "2025-11-15"
  }' | jq .

# Validation failure on update — title too long
curl -s -X PATCH "$BASE/api/tasks/1" \
  -H "Content-Type: application/json" \
  -d '{"title": "'"$(python3 -c "print('x'*256)")"'"}' | jq .

# ── DELETE ────────────────────────────────────────────────────────────────────

# Delete — expect 204 No Content (no body)
curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/api/tasks/1"
echo ""

# Delete already-deleted → 404
curl -s -X DELETE "$BASE/api/tasks/1" | jq .

# ── AI ────────────────────────────────────────────────────────────────────────

# Suggest sub-tasks for a goal
curl -s -X POST "$BASE/api/ai/suggest" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "I need to plan a product launch for Q4"}' | jq .

# Blank prompt → 400 validation error
curl -s -X POST "$BASE/api/ai/suggest" \
  -H "Content-Type: application/json" \
  -d '{"prompt": ""}' | jq .

# ── INSPECT HEADERS ───────────────────────────────────────────────────────────

# Check Location header on create — should be /api/tasks/{id}
curl -si -X POST "$BASE/api/tasks" \
  -H "Content-Type: application/json" \
  -d '{"title": "Check my Location header"}' | grep -i "^location:"
