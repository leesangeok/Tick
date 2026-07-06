package app.tick.news.adapter.external

import app.tick.common.domain.StockCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 종목코드 (6자리) → DART `corp_code` (8자리) 매핑.
 *
 * opendart 표준 방식: `corpCode.xml` 을 한번 다운로드 (ZIP + XML) → stock_code 가 6자리 숫자인
 * 상장사만 메모리 캐시. Lazy load — 첫 lookup 요청 시 로드해서 앱 부팅을 늦추지 않는다.
 *
 * 실패 (네트워크/키 문제) 시 빈 map 을 캐싱. 조회 실패마다 재다운로드하면 rate limit 에 걸리므로
 * 재시도는 앱 재시작 시점까지 미룬다.
 */
@Component
class DartCorpCodeMap(
    private val properties: DartProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var cached: Map<String, String>? = null

    fun lookup(stockCode: StockCode): String? = ensure()[stockCode.value]

    fun size(): Int = ensure().size

    private fun ensure(): Map<String, String> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val loaded = try {
                if (properties.apiKey.isBlank()) {
                    log.info("DART api key blank — corp_code map stays empty")
                    emptyMap()
                } else {
                    downloadAndParse()
                }
            } catch (e: Exception) {
                log.warn("DART CORPCODE download failed: {}", e.message)
                emptyMap()
            }
            cached = loaded
            log.info("DART corp_code map cached: {} entries", loaded.size)
            return loaded
        }
    }

    private fun downloadAndParse(): Map<String, String> {
        val bytes = RestClient.create().get()
            .uri("${properties.baseUrl}/corpCode.xml?crtfc_key={key}", properties.apiKey)
            .retrieve()
            .body(ByteArray::class.java)
            ?: throw IOException("empty body")

        // 정상 응답은 ZIP (0x50 0x4B). 오류는 XML/JSON.
        val isZip = bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        if (!isZip) throw IOException("non-zip response: ${String(bytes.take(200).toByteArray())}")

        val xml = unzipFirstXml(bytes)
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xml))

        val list = doc.getElementsByTagName("list")
        val map = HashMap<String, String>(list.length.coerceAtLeast(16))
        for (i in 0 until list.length) {
            val el = list.item(i) as? Element ?: continue
            val stockCode = el.firstText("stock_code")
            val corpCode = el.firstText("corp_code")
            if (stockCode.length == 6 && stockCode.all(Char::isDigit) && corpCode.length == 8) {
                map[stockCode] = corpCode
            }
        }
        return map
    }

    private fun unzipFirstXml(bytes: ByteArray): ByteArray {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".xml", ignoreCase = true)) return zis.readAllBytes()
                entry = zis.nextEntry
            }
            throw IOException("no .xml entry in DART ZIP")
        }
    }

    private fun Element.firstText(tag: String): String {
        val nodes = getElementsByTagName(tag)
        return if (nodes.length > 0) nodes.item(0).textContent?.trim().orEmpty() else ""
    }
}
