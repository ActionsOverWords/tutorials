package tutorials.lgtm.config

import io.pyroscope.http.Format
import io.pyroscope.javaagent.EventType
import io.pyroscope.javaagent.PyroscopeAgent
import io.pyroscope.javaagent.config.Config
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "pyroscope")
data class PyroscopeProperties(
  var applicationName: String = "",
  var format: String = "",
  var serverAddress: String = "",
  var profilingAlloc: String? = null,
  var profilingLock: String? = null,
)

@Component
class PyroscopeConfiguration(
  private val properties: PyroscopeProperties
) {

  @PostConstruct
  fun initPyroscope() {
    val configBuilder = Config.Builder()
      .setApplicationName(properties.applicationName)
      .setProfilingEvent(EventType.ITIMER)
      .setFormat(Format.valueOf(properties.format))
      .setServerAddress(properties.serverAddress)
      .setProfilingAlloc(properties.profilingAlloc)
      .setProfilingLock(properties.profilingLock)

    PyroscopeAgent.start(configBuilder.build())
  }

}
