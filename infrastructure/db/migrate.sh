#!/usr/bin/env bash
set -e

docker run --rm \
  --network=readscape_network \
  -v "$(pwd)/../database/migrations":/flyway/sql \
  flyway/flyway \
  -url=jdbc:postgresql://readscape_rdb:5432/readscapedb \
  -user=readscapeapp \
  -password=passwd \
  -schemas=readscape \
  -locations=filesystem:/flyway/sql \
  migrate
