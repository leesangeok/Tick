package app.tick.ai.application

import app.tick.common.domain.StockCode

/**
 * Python ai-server 와의 통신을 추상화.
 * docker 내부 네트워크에서 http://ai-server:8000 호출.
 */
interface AiServerPort {
    fun summarize(stockCode: StockCode, stockName: String): AiSummaryResult
    fun embed(stockCode: StockCode): EmbedResult
}
