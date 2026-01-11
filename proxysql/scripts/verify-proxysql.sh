#!/bin/bash

echo "=== ProxySQL 서버 설정 ==="
docker exec proxysql mysql -h127.0.0.1 -P6032 -uadmin -padmin -e "SELECT hostgroup_id, hostname, port, status FROM mysql_servers" --table

echo -e "\n=== 쿼리 라우팅 규칙 ==="
docker exec proxysql mysql -h127.0.0.1 -P6032 -uadmin -padmin -e "SELECT rule_id, match_digest, destination_hostgroup, apply, comment FROM mysql_query_rules ORDER BY rule_id" --table

echo -e "\n=== 통계 초기화 ==="
docker exec proxysql mysql -h127.0.0.1 -P6032 -uadmin -padmin -e "SELECT * FROM stats_mysql_query_digest_reset LIMIT 1" > /dev/null

echo -e "\n테스트를 실행하세요: ./gradlew test --tests NoticeServiceTest"
read -p "Press enter after running tests..."

echo -e "\n=== 쿼리 통계 (hostgroup별) ==="
docker exec proxysql mysql -h127.0.0.1 -P6032 -uadmin -padmin -e "
SELECT
  hostgroup,
  SUBSTRING(digest_text, 1, 100) as query,
  count_star
FROM stats_mysql_query_digest
WHERE digest_text LIKE '%notice%'
ORDER BY hostgroup, count_star DESC
LIMIT 30" --table
