package tutorials.pdf.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class ExternalClient(
  private val externalProperties: ExternalProperties,
) {

  @Bean
  fun quickChartClient(builder: RestClient.Builder): QuickChartClient {
    return builder.toExchange(externalProperties.quickchartUrl)
  }

  private inline fun <reified T> RestClient.Builder.toExchange(baseUrl: String): T {
    val restClient = this.baseUrl(baseUrl)
      .build()

    val adapter = RestClientAdapter.create(restClient)
    val factory = HttpServiceProxyFactory.builderFor(adapter)
      .build()

    return factory.createClient(T::class.java)
  }

  @Bean
  fun gotenbergClient(builder: RestClient.Builder): GotenbergClient {
    return builder.toExchange(externalProperties.gotenbergUrl)
  }

}
