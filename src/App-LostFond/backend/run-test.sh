#!/usr/bin/env bash
cd "D:/Visual_Stdio_Code/Dai_ma/软件工程/App-LostFond/backend"
export JAVA_HOME="D:/Visual_Stdio_Code/Java_peizhi/JDK21"
export APP_DATA_DIR="$TEMP/shiguang-app-test"
export JWT_SECRET="test-only-secret-0123456789-abcdefghijklmnopqrstuvwxyz"
export SERVER_PORT=18080
java -jar target/lost-found-backend.jar --spring.profiles.active=app
