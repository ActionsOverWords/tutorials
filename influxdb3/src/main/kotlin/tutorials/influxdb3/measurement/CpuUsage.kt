package tutorials.influxdb3.measurement

import tutorials.influxdb3.base.measurement.AbstractMeasurement
import tutorials.influxdb3.base.measurement.MeasurementConst.CPU_USAGE
import java.time.Instant

class CpuUsage(
  time: Instant = Instant.now(),
) : AbstractMeasurement(CPU_USAGE, time) {

  fun setArchitecture(architecture: String): CpuUsage {
    addTag("architecture", architecture)
    return this
  }

  fun setUsagePercent(usage: Double): CpuUsage {
    addField("usage_percent", usage)
    return this
  }

}
