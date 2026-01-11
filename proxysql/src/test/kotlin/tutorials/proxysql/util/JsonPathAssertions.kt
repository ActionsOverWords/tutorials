package tutorials.proxysql.util

import org.assertj.core.api.AssertProvider
import org.springframework.test.json.JsonPathValueAssert
import java.util.function.Consumer

fun isNotNull() = Consumer<AssertProvider<JsonPathValueAssert>> {
  it.assertThat().isNotNull()
}

fun <T> isEqualTo(expected: T) = Consumer<AssertProvider<JsonPathValueAssert>> {
  it.assertThat().isEqualTo(expected)
}
