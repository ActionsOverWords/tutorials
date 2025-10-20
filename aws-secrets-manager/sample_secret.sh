#!/bin/bash

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# LocalStack 설정
CONTAINER_NAME="localstack"
LOCALSTACK_ENDPOINT="http://localhost:4566"
MAX_RETRY=30
RETRY_COUNT=0

# 스크립트가 있는 디렉토리로 이동
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${GREEN}=== LocalStack Secrets Manager Setup ===${NC}"
echo -e "${YELLOW}Working directory: $SCRIPT_DIR${NC}\n"

# Docker 실행 확인
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

# compose.yml 파일 존재 확인
if [ ! -f "compose.yml" ]; then
    echo -e "${RED}Error: compose.yml not found in current directory${NC}"
    exit 1
fi

# LocalStack 컨테이너 상태 확인
check_container_status() {
    docker ps --filter "name=${CONTAINER_NAME}" --format "{{.Status}}" 2>/dev/null
}

# LocalStack이 준비되었는지 확인
wait_for_localstack() {
    echo -e "${YELLOW}Waiting for LocalStack to be ready...${NC}"

    while [ $RETRY_COUNT -lt $MAX_RETRY ]; do
        # 먼저 컨테이너가 실행 중인지 확인
        if ! docker ps --filter "name=${CONTAINER_NAME}" --format "{{.Names}}" | grep -q "${CONTAINER_NAME}"; then
            echo -e "\n${RED}✗ LocalStack container is not running${NC}"
            return 1
        fi

        # Health endpoint 응답 확인
        HEALTH_RESPONSE=$(curl -s "${LOCALSTACK_ENDPOINT}/_localstack/health" 2>/dev/null || echo "")

        if [ -n "$HEALTH_RESPONSE" ]; then
            # grep으로 secretsmanager 상태 추출
            SERVICE_STATUS=$(echo "$HEALTH_RESPONSE" | grep -o '"secretsmanager"[[:space:]]*:[[:space:]]*"[^"]*"' | cut -d'"' -f4)

            if [ "$SERVICE_STATUS" = "available" ] || [ "$SERVICE_STATUS" = "running" ]; then
                echo -e "\n${GREEN}✓ LocalStack is ready! (secretsmanager: $SERVICE_STATUS)${NC}"
                return 0
            elif [ -z "$SERVICE_STATUS" ]; then
                # secretsmanager가 아직 응답에 없는 경우
                echo -n "."
            else
                echo -n "."
            fi
        else
            # 아직 응답이 없는 경우
            echo -n "."
        fi

        RETRY_COUNT=$((RETRY_COUNT + 1))
        sleep 2
    done

    echo -e "\n${RED}✗ LocalStack failed to start within the expected time${NC}"
    echo -e "${YELLOW}Debug: Last health response:${NC}"
    curl -s "${LOCALSTACK_ENDPOINT}/_localstack/health" 2>/dev/null || echo "No response"
    echo -e "\n${YELLOW}Container logs:${NC}"
    docker logs --tail 20 "${CONTAINER_NAME}"
    return 1
}

# LocalStack 컨테이너 내부에서 awslocal 명령어 실행
exec_awslocal() {
    docker exec -i "${CONTAINER_NAME}" awslocal "$@"
}

# LocalStack 컨테이너 확인 및 시작
CONTAINER_STATUS=$(check_container_status)

if [ -z "$CONTAINER_STATUS" ]; then
    echo -e "${YELLOW}LocalStack container is not running. Starting LocalStack...${NC}"

    # LocalStack 시작
    docker compose up -d

    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Failed to start LocalStack${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ LocalStack container started${NC}"
    sleep 3  # 컨테이너 시작 후 초기화 시간 대기
else
    echo -e "${GREEN}✓ LocalStack is already running (${CONTAINER_STATUS})${NC}"
fi

# LocalStack이 준비될 때까지 대기
if ! wait_for_localstack; then
    echo -e "${RED}Error: LocalStack is not responding${NC}"
    echo -e "${YELLOW}Try running: docker compose logs localstack${NC}"
    exit 1
fi

echo -e "\n${GREEN}Initializing Secrets Manager...${NC}"

# 기존 시크릿 삭제 (있을 경우)
echo "Cleaning up existing secrets..."
exec_awslocal secretsmanager delete-secret --secret-id /tutorials/api-key --force-delete-without-recovery 2>/dev/null || true
exec_awslocal secretsmanager delete-secret --secret-id /tutorials/api-key-json --force-delete-without-recovery 2>/dev/null || true
exec_awslocal secretsmanager delete-secret --secret-id tutorial-key --force-delete-without-recovery 2>/dev/null || true

sleep 1

# API Key 시크릿 생성
echo "Creating API key secret..."
exec_awslocal secretsmanager create-secret \
    --name /tutorials/api-key \
    --description "API credentials" \
    --secret-string 'tutorials'

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ API key secret created successfully${NC}"
else
    echo -e "${RED}✗ Failed to create API key secret${NC}"
    exit 1
fi

echo "Creating API(json) key secret..."
exec_awslocal secretsmanager create-secret \
    --name /tutorials/api-key-json \
    --description "API credentials" \
    --secret-string '{"apiKey":"tutorials-api-key","apiSecret":"tutorials-secret"}'

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ API key secret created successfully${NC}"
else
    echo -e "${RED}✗ Failed to create API key secret${NC}"
    exit 1
fi

echo "Creating API key secret..."
exec_awslocal secretsmanager create-secret \
    --name tutorial-key \
    --description "API credentials" \
    --secret-string 'tutorial-key'

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ API key secret created successfully${NC}"
else
    echo -e "${RED}✗ Failed to create API key secret${NC}"
    exit 1
fi

# 생성된 시크릿 목록 확인
echo -e "\n${GREEN}=== Created Secrets ===${NC}"
exec_awslocal secretsmanager list-secrets --query 'SecretList[*].[Name,Description]' --output table
