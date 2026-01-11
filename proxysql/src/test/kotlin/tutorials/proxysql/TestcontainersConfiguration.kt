package tutorials.proxysql

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * Testcontainer 설정
 *
 * @TestConfiguration은 자동 스캔되지 않으며, @Import로 명시적으로 가져온 테스트에서만 사용됨
 * - AbstractRepositoryTest, AbstractIntegrationTest를 상속한 테스트: Testcontainer 사용
 * - ProxySQLVerificationTest (@Import 없음): 실제 ProxySQL 연결 (application-integration.yml)
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  fun mariaDbContainer(): MariaDBContainer<*> {
    return MariaDBContainer(DockerImageName.parse("mariadb:11.7"))
  }

}
