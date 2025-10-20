package tutorials.influxdb3.base.repository

import org.apache.arrow.flight.CallOption
import org.apache.arrow.flight.FlightClient
import org.apache.arrow.flight.FlightDescriptor
import org.apache.arrow.flight.SyncPutListener
import org.apache.arrow.vector.VectorSchemaRoot

abstract class ApacheArrowSaveRepository(
  private val flightClient: FlightClient,
  private val callOption: CallOption
) {

  fun writeBatch(measurement: String, root: VectorSchemaRoot) {
    val descriptor = FlightDescriptor.path(measurement)
    val listener = SyncPutListener()

    val stream = flightClient.startPut(descriptor, root, listener, callOption)

    //todo: 사용 방법
    stream.putNext()
    stream.completed()
    stream.getResult()
  }

}
