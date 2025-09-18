package tutorials.influxdb3.base.measurement

import java.math.BigInteger

object MeasurementConst {
  const val CPU_USAGE = "cpu_usage"
  val NANOS_PER_SECOND: BigInteger = BigInteger.valueOf(1_000_000_000)
}
