package tutorials.measurement

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import tutorials.base.consts.MeasurementConst.SIMPLE
import java.time.Instant

@Measurement(name = SIMPLE)
data class SimpleMeasurement(
  @Column(timestamp = true) val time: Instant? = Instant.now(),

  @Column(tag = true) val country: String = "",

  @Column val temperature: Double = 0.0,
)
