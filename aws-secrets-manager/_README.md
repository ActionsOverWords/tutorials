# AWS Secrets manager

## 1. LocalStack
### 1.1. LocalStack이란?
- WS 클라우드 서비스를 로컬 환경에서 에뮬레이트하는 오픈소스 도구입니다. 실제 AWS에 연결하지 않고도 로컬 개발 환경에서 AWS 서비스를 테스트하고 개발할 수 있게 해줌

### 1.2. [Starting LocalStack with Docker-Compose](https://docs.localstack.cloud/aws/getting-started/installation/#docker-compose)
```docker
localstack:
  container_name: localstack
  image: localstack/localstack:4.9.2
  ports:
    - "4566:4566"            # LocalStack Gateway
    - "4510-4559:4510-4559"  # 외부 서비스 포트 범위
  environment:
    # LocalStack configuration: https://docs.localstack.cloud/references/configuration/
    - SERVICES=secretsmanager
    - DEBUG=${DEBUG:-0}
  volumes:
    - ./docker/localstack:/var/lib/localstack
    - /var/run/docker.sock:/var/run/docker.sock:ro
```

- [Docker LocalStack: Health Check](http://localhost:4566/_localstack/health)

- [Loading External Configuration](https://docs.awspring.io/spring-cloud-aws/docs/3.4.0/reference/html/index.html#loading-external-configuration-2)

# 참고
- [Spring Cloud AWS](https://github.com/awspring/spring-cloud-aws)
- [LocalStack: Secrets Manager](https://docs.localstack.cloud/aws/services/secretsmanager/)
- [Testcontainers: LocalStack](https://testcontainers.com/modules/localstack/)
