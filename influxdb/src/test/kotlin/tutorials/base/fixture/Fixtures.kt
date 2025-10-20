package tutorials.base.fixture

import tutorials.measurement.Sensor
import tutorials.measurement.SimpleMeasurement
import kotlin.random.Random

class Fixtures {

  companion object {
    fun createBySimple() = SimpleMeasurement(
      country = "KR",
      temperature = 31.5,
    )

    fun createBySensor() = createSensor(
      country = "KR",
      location = "ICN"
    )

    fun createSensorByKr() = createSensor(
      country = "KR",
      location = "SEL",
      temperature = 31.5,
      humidity = 33.0
    )

    fun createSensorByUs() = createSensor(
      country = "US",
      location = "SF",
      temperature = 23.5,
      humidity = 28.5
    )

    fun createSensor(country: String, location: String, temperature: Double? = null, humidity: Double? = null) = Sensor(
      country = country,
      location = location,
      temperature = temperature ?: randomDouble(),
      humidity = humidity,
    )

    private fun randomDouble() = Random.nextDouble(-20.0, 50.0)
  }

}
