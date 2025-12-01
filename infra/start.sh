#!/bin/bash

echo "▶ Starting Backend Instance 1 on 8081..."
java -jar app.jar --server.port=8081 &

echo "▶ Starting Backend Instance 2 on 8082..."
java -jar app.jar --server.port=8082 &

sleep 5
echo "▶ Starting Nginx Load Balancer..."
nginx -g "daemon off;"
