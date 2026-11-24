@echo off
set COTAI_PROXY_ENABLED=true
set PROXY_USERNAME=cotai_proxy
set PROXY_PASSWORD=cotai_proxy_password_123456
set JAVA_TOOL_OPTIONS=-Djdk.http.auth.tunneling.disabledSchemes=
call .\mvnw.cmd spring-boot:run > backend.log 2>&1
