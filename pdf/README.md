# Server-Side PDF Generation

서버 사이드에서 PDF를 생성하는 두 가지 방식을 비교하는 샘플 프로젝트

## 개요

- 웹 애플리케이션에서 리포트나 문서를 PDF로 생성해야 할 때, HTML을 PDF로 변환하는 방식이 일반적
- **OpenHTMLToPDF**와 **Gotenberg** 두 가지 방식을 비교

## OpenHTMLToPDF vs Gotenberg

### OpenHTMLToPDF

**동작 방식**:
1. Thymeleaf로 HTML 템플릿 렌더링
2. OpenHTMLToPDF 라이브러리로 HTML → PDF 변환

```kotlin
@Component
class OpenHtmlToPdfGenerator(
  val thymeleafTemplateService: ThymeleafTemplateService,
  val quickChartService: QuickChartService,
) : AbstractPdfGenerator() {

  override fun renderHtml(report: Report): String {
    return thymeleafTemplateService.renderHtml(HtmlTemplate.OpenHtmlToPdf, report)
  }

  override fun convertToPdf(html: String): ByteArray {
    ByteArrayOutputStream().use { outputStream ->
      val builder = PdfRendererBuilder()
      builder.useFastMode()
      builder.withHtmlContent(html, null)
      builder.toStream(outputStream)
      loadFonts(builder)  // 한글 폰트 로딩
      builder.run()
      return outputStream.toByteArray()
    }
  }
}
```

**장점**:
- 외부 Docker 서비스 없이 순수 Java 라이브러리로 동작
- 가볍고 빠른 실행 속도
- 리소스 사용량이 적음
- 안정적이고 예측 가능한 결과

**단점**:
- JavaScript 실행 불가 (정적 HTML만 지원)
- 차트를 이미지로 변환하는 추가 과정 필요 (QuickChart API 호출)
- 한글 폰트를 수동으로 임베딩해야 함
- CSS 지원 범위가 제한적 (일부 모던 CSS 미지원)

**적합한 사용 케이스**:
- 리소스가 제한적인 환경 (낮은 메모리/CPU)
- 간단한 레이아웃과 스타일의 문서

### Gotenberg

**동작 방식**:
1. Thymeleaf로 HTML 템플릿 렌더링 (Chart.js 스크립트 포함)
2. HTML을 Gotenberg API로 전송
3. Gotenberg가 Chromium으로 HTML을 렌더링하고 PDF 생성

```kotlin
@Component
class GotenbergPdfGenerator(
  val thymeleafTemplateService: ThymeleafTemplateService,
  private val gotenbergClient: GotenbergClient,
) : AbstractPdfGenerator() {

  override fun renderHtml(report: Report): String {
    return thymeleafTemplateService.renderHtml(HtmlTemplate.Gotenberg, report)
  }

  override fun convertToPdf(html: String): ByteArray {
    val htmlResource = ByteArrayResource(html.toByteArray(Charsets.UTF_8))
    return gotenbergClient.generatePdf(htmlResource)
  }
}
```

**Gotenberg API 클라이언트** (Spring HTTP Interface):

```kotlin
interface GotenbergClient {

  @PostExchange("/forms/chromium/convert/html")
  fun generatePdf(
    @RequestPart("files") html: Resource,
    @RequestParam("waitDelay") waitDelay: String = "2s",  // 차트 렌더링 대기
  ): ByteArray
}
```

**장점**:
- JavaScript 완전 지원 (Chart.js 직접 사용 가능)
- 웹 브라우저와 동일한 렌더링 결과 (WYSIWYG)
- 모던 CSS 완벽 지원 (Flexbox, Grid 등)
- 한글 폰트 자동 지원 (시스템 폰트 사용)
- 복잡한 레이아웃과 스타일링 가능

**단점**:
- Docker 컨테이너 필요 (추가 인프라 관리)
- 높은 리소스 사용량 (Chromium 실행)
- 상대적으로 느린 실행 속도

**적합한 사용 케이스**:
- 복잡한 레이아웃과 인터랙티브한 차트가 필요한 경우
- 웹 페이지와 동일한 렌더링 결과가 중요한 경우

## 비교 요약

| 항목             | OpenHTMLToPDF        | Gotenberg       |
|----------------|----------------------|-----------------|
| **실행 환경**      | Java 라이브러리           | Docker 컨테이너     |
| **JavaScript** | 미지원                  | 완전 지원           |
| **차트 생성**      | QuickChart API (이미지) | Chart.js (네이티브) |
| **한글 폰트**      | 수동 임베딩 필요            | 자동 지원           |
| **CSS 지원**     | 제한적                  | 완벽 지원           |
| **렌더링 엔진**     | Flying Saucer        | Chromium        |
| **리소스 사용**     | 낮음                   | 높음              |
| **실행 속도**      | 빠름                   | 느림              |

## 한글 폰트 설정 (OpenHTMLToPDF)

OpenHTMLToPDF는 기본적으로 한글을 지원하지 않아, TTF 폰트 파일을 임베딩

### 1. 폰트 로딩 구현

```kotlin
private fun loadFonts(builder: PdfRendererBuilder) {
  val regularFontResource = ClassPathResource("static/font/NanumGothic-Regular.ttf")
  val boldFontResource = ClassPathResource("static/font/NanumGothic-Bold.ttf")

  val regularFontBytes = regularFontResource.inputStream.use { it.readBytes() }
  val boldFontBytes = boldFontResource.inputStream.use { it.readBytes() }

  builder.useFont({ regularFontBytes.inputStream() }, "NanumGothic")
  builder.useFont({ boldFontBytes.inputStream() }, "NanumGothic-Bold")
}
```

### 2. CSS에서 폰트 적용

```css
body {
  font-family: 'NanumGothic', sans-serif;
}

strong, b {
  font-family: 'NanumGothic-Bold', sans-serif;
}
```

**주의사항**:
- 폰트 로딩은 `builder.run()` 호출 전에 완료되어야 함
- `useFont()`의 두 번째 파라미터는 CSS `font-family`와 일치해야 함

## 차트 처리 방식 비교

### OpenHTMLToPDF: QuickChart API

```kotlin
@Service
class QuickChartService(
  private val quickChartClient: QuickChartClient,
) {
  fun generateChartImage(chart: Chart): String {
    val request = QuickChartRequest(
      backgroundColor = "white",
      width = 500,
      height = 300,
      format = "png",
      chart = buildChartConfig(chart)
    )

    val response = quickChartClient.generateChart(request)
    return "data:image/png;base64,${response.toBase64()}"
  }
}
```

HTML에서 사용:
```html
<img src="data:image/png;base64,${imageBase64String}" />
```

**특징**:
- Chart.js 설정을 JSON으로 전송하면 PNG 이미지 반환
- Base64로 인코딩하여 HTML에 직접 삽입
- 정적 이미지이므로 JavaScript 없이 렌더링 가능

### Gotenberg: Chart.js 직접 렌더링

HTML 템플릿에 Chart.js를 포함하여 Chromium이 렌더링

```html
<canvas id="myChart"></canvas>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
  new Chart(document.getElementById('myChart'), {
    type: 'bar',
    data: {
      labels: [/*...*/],
      datasets: [/*...*/]
    }
  });
</script>
```

**특징**:
- JavaScript가 실행되어 캔버스에 차트 그림
- 브라우저에서 보이는 것과 동일한 결과

## 선택 가이드
- 개발 생산성과 유지보수성이 우선인 경우

## 참고 자료

- [OpenHTMLToPDF](https://github.com/danfickle/openhtmltopdf) - Java HTML to PDF 라이브러리
- [Gotenberg](https://gotenberg.dev/) - Docker 기반 PDF 생성 서비스
- [QuickChart](https://quickchart.io/) - Chart.js 이미지 생성 API
- [Chart.js](https://www.chartjs.org/) - JavaScript 차트 라이브러리
