-- Slave 서버 초기화 스크립트

-- ProxySQL 모니터링 사용자 생성
CREATE USER IF NOT EXISTS 'monitor'@'%' IDENTIFIED BY 'monitor';
GRANT USAGE, REPLICATION CLIENT ON *.* TO 'monitor'@'%';

FLUSH PRIVILEGES;

-- Master 연결 설정
-- docker-compose에서 depends_on으로 master가 healthy 상태가 된 후 실행됨
CHANGE MASTER TO
  MASTER_HOST='mariadb-master',
  MASTER_USER='repl',
  MASTER_PASSWORD='repl_password',
  MASTER_PORT=3306,
  MASTER_CONNECT_RETRY=10;

-- Replication 시작
START SLAVE;
