package tutorials.proxysql.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import tutorials.proxysql.base.extentions.logger
import tutorials.proxysql.dto.NoticeRequest
import tutorials.proxysql.service.NoticeService
import kotlin.test.assertTrue

/**
 * ProxySQL read/write 라우팅 검증 테스트
 *
 * 주의:
 * - docker-compose로 ProxySQL이 실행 중이어야 합니다 (docker-compose up -d)
 * - @Transactional을 사용하지 않음 (독립적인 트랜잭션으로 실행되어야 ProxySQL 분기가 작동)
 * - @ActiveProfiles("integration")으로 testcontainer 자동 설정을 비활성화하고 실제 ProxySQL 사용
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ActiveProfiles("integration")
class ProxySQLVerificationTest(
  private val noticeService: NoticeService,
) {

  private val log by logger()

  @BeforeEach
  fun setUp() {
    // 통계 초기화
    executeProxySQLQuery("SELECT * FROM stats_mysql_query_digest_reset LIMIT 1")
  }

  private fun executeProxySQLQuery(query: String): String {
    val command = arrayOf(
      "docker", "exec", "proxysql",
      "mysql", "-h127.0.0.1", "-P6032", "-uadmin", "-padmin",
      "-e", query
    )
    val process = ProcessBuilder(*command)
      .redirectErrorStream(true)
      .start()

    return process.inputStream.bufferedReader().use { it.readText() }
      .also { process.waitFor() }
  }

  @Test
  fun `ProxySQL read write 분기 검증`() {
    // given: 통계 초기화 완료

    // when: write 작업 수행 (INSERT) - 각각 독립적인 트랜잭션
    val saved1 = noticeService.save(NoticeRequest(title = "공지1", content = "내용1"))
    noticeService.save(NoticeRequest(title = "공지2", content = "내용2"))

    // when: read 작업 수행 (SELECT) - 독립적인 트랜잭션, @Transactional(readOnly=true)로 실행
    noticeService.findAll()
    noticeService.findActiveNotices()

    // when: update 작업 수행 - 독립적인 트랜잭션
    noticeService.update(saved1.id, NoticeRequest(title = "수정됨", content = "수정된 내용"))

    // then: ProxySQL 통계 확인
    val statsOutput = executeProxySQLQuery(
      """
      SELECT
        hostgroup,
        digest_text,
        count_star
      FROM stats_mysql_query_digest
      WHERE digest_text LIKE '%notice%'
      ORDER BY hostgroup, count_star DESC
      """.trimIndent()
    )

    log.info("\n=== ProxySQL 쿼리 통계 ===")
    log.info(statsOutput)

    // 검증: hostgroup별 쿼리 분류
    // Master (hostgroup 0): INSERT, UPDATE 쿼리가 있어야 함
    val hasMasterInsert = statsOutput.contains("0\t") && statsOutput.contains("insert", ignoreCase = true)
    val hasMasterUpdate = statsOutput.contains("0\t") && statsOutput.contains("update", ignoreCase = true)

    // Slave (hostgroup 1): SELECT 쿼리가 있어야 함
    val hasSlaveSelect = statsOutput.contains("1\t") && statsOutput.contains("select", ignoreCase = true)

    assertTrue(hasMasterInsert, "Master(hostgroup 0)에 INSERT 쿼리가 없습니다\n$statsOutput")
    assertTrue(hasMasterUpdate, "Master(hostgroup 0)에 UPDATE 쿼리가 없습니다\n$statsOutput")
    assertTrue(hasSlaveSelect, "Slave(hostgroup 1)에 SELECT 쿼리가 없습니다\n$statsOutput")

    log.info("\n✅ ProxySQL read/write 분기가 정상 작동합니다!")
    log.info("  - Master (hostgroup 0): INSERT, UPDATE 쿼리 존재")
    log.info("  - Slave (hostgroup 1): SELECT 쿼리 존재")
  }

  @Test
  fun `ProxySQL 서버 상태 확인`() {
    // ProxySQL 서버 설정 확인
    val serversOutput = executeProxySQLQuery(
      "SELECT hostgroup_id, hostname, port, status FROM mysql_servers"
    )

    log.info("\n=== ProxySQL 서버 설정 ===")
    log.info(serversOutput)

    // 검증: master와 slave가 모두 ONLINE 상태여야 함
    assertTrue(serversOutput.contains("mariadb-master"), "mariadb-master 서버가 없습니다")
    assertTrue(serversOutput.contains("mariadb-slave"), "mariadb-slave 서버가 없습니다")
    assertTrue(serversOutput.contains("ONLINE"), "서버가 ONLINE 상태가 아닙니다")

    log.info("\n✅ ProxySQL 서버가 정상 작동 중입니다!")
  }
}
