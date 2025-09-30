package tutorials.influxdb

import com.influxdb.client.QueryApi
import com.influxdb.client.WriteApiBlocking
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.reactive.QueryReactiveApi
import com.influxdb.client.reactive.WriteReactiveApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import io.reactivex.rxjava3.core.Flowable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import tutorials.base.consts.MeasurementConst.SIMPLE
import tutorials.base.fixture.Fixtures
import tutorials.config.AbstractContainerTest
import tutorials.config.InfluxDB2TestContainerConfig.Companion.BUCKET
import tutorials.measurement.Sensor
import java.time.Instant
import java.time.temporal.ChronoUnit

class InfluxDbTest : AbstractContainerTest() {

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class BlockingTest {

    lateinit var writeApi: WriteApiBlocking
    lateinit var queryApi: QueryApi

    @BeforeEach
    fun setup() {
      writeApi = influxDb2Client.writeApiBlocking
      queryApi = influxDb2Client.queryApi
    }

    @Test
    fun save() {
      val measurement = Fixtures.createBySimple()
      log.debug("{}", measurement)

      assertDoesNotThrow {
        writeApi.writeMeasurement(WritePrecision.MS, measurement)
      }
    }

    @Test
    fun saves() {
      val measurements = listOf(
        Fixtures.createSensorByKr(),
        Fixtures.createSensorByUs(),
      )
      log.debug("{}", measurements)

      assertDoesNotThrow {
        writeApi.writeMeasurements(WritePrecision.MS, measurements)
      }
    }

    @Test
    fun query() {
      val measurement = Fixtures.createBySensor()
      log.debug("{}", measurement)
      writeApi.writeMeasurement(WritePrecision.MS, measurement)

      val fluxQuery = createQueryFlux(measurement)
      log.debug("{}", fluxQuery)

      val sensors = queryApi.query(fluxQuery.toString(), Sensor::class.java)

      assertAll(
        { assertTrue(sensors.isNotEmpty()) },
        { assertEquals(measurement.country, sensors.first().country) },
      )
    }

  }


  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class ReactiveTest {

    lateinit var writeReactiveApi: WriteReactiveApi
    lateinit var reactiveQueryApi: QueryReactiveApi

    @BeforeEach
    fun setup() {
      writeReactiveApi = influxDb2ClientReactive.writeReactiveApi
      reactiveQueryApi = influxDb2ClientReactive.queryReactiveApi
    }

    @Test
    fun save() {
      val measurement = Fixtures.createBySimple()
      log.debug("{}", measurement)

      val saveOperation = save(measurement)

      StepVerifier.create(saveOperation)
        .verifyComplete()
    }

    @Test
    fun saves() {
      val measurements = listOf(
        Fixtures.createSensorByKr(),
        Fixtures.createSensorByUs(),
      )
      log.debug("{}", measurements)

      val saveOperation = save(measurements)

      StepVerifier.create(saveOperation)
        .verifyComplete()
    }

    private fun save(measurement: Any): Mono<Void> {
      return save(listOf(measurement))
    }

    private fun save(measurements: List<Any>): Mono<Void> {
      val flowable = Flowable.fromIterable(measurements)
      val publisher = writeReactiveApi.writeMeasurements(WritePrecision.MS, flowable)

      return Mono.from(publisher).then()
    }

    @Test
    fun query() {
      val measurement = Fixtures.createBySensor()
      log.debug("{}", measurement)
      val saveOperation = save(measurement)

      val fluxQuery = createQueryFlux(measurement)
      log.debug("{}", fluxQuery)

      val queryPublisher = reactiveQueryApi.query(fluxQuery.toString(), Sensor::class.java)

      val combinedFlux = saveOperation.thenMany(queryPublisher)

      StepVerifier.create(combinedFlux)
        .assertNext { sensor ->
          assert(sensor.country == measurement.country)
        }
        .thenConsumeWhile { true }
        .verifyComplete()
    }

  }

  private fun createQueryFlux(measurement: Sensor): Flux {
    val restrictions = Restrictions.and(
      Restrictions.measurement().equal(SIMPLE),
      Restrictions.tag("country").equal(measurement.country),
    )

    return Flux.from(BUCKET)
      .range(getInstant(-1), getInstant(0))
      .filter(restrictions)
      .sort(listOf("_time"))
  }

  private fun getInstant(offsetHour: Int): Instant {
    return Instant.now().plus(offsetHour.toLong(), ChronoUnit.HOURS)
  }
}
