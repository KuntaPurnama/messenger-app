#!/bin/bash
MIGRATIONS_DIR="src/main/resources/db/migration"

for file in "$MIGRATIONS_DIR"/*.sql; do
  filename=$(basename "$file")
  if [[ ! $filename =~ ^V[0-9]+__ ]]; then
      TIMESTAMP=$(date +%s%3N)
      mv "$file" "$MIGRATIONS_DIR/V${TIMESTAMP}__${filename}"
      echo "Renamed: $filename -> V${TIMESTAMP}__${filename}"
      sleep 0.01
  fi
done