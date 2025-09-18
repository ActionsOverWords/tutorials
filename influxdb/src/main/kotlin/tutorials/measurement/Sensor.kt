package tutorials.measurement

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import tutorials.base.consts.MeasurementConst.SENSOR
import java.time.Instant

@Measurement(name = SENSOR)
data class Sensor(

  @Column(timestamp = true) val time: Instant? = Instant.now(),

  @Column(tag = true) val country: String = "",

  @Column(tag = true) val location: String = "",

  @Column val temperature: Double = 0.0,

  @Column val humidity: Double? = null,

)
