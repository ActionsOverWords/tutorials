package tutorials.pdf.base.extentions

fun String.normalizeWhitespace() = this
  .replace("\n", "")
  .replace(Regex("\\s+"), " ")
