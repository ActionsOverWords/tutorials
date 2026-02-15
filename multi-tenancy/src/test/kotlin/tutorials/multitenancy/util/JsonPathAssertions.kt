package tutorials.multitenancy.util

import org.assertj.core.api.AssertProvider
import org.springframework.test.json.JsonPathValueAssert
import java.util.function.Consumer

fun <T> isEqualTo(expected: T) = Consumer<AssertProvider<JsonPathValueAssert>> {
  it.assertThat().isEqualTo(expected)
}

fun isNotBlank() = Consumer<AssertProvider<JsonPathValueAssert>> {
  it.assertThat().asString().isNotBlank()
}
