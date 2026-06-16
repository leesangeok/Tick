package app.tick.ai.application

import app.tick.common.domain.StockCode
import app.tick.common.exception.BusinessException
import app.tick.common.exception.ErrorCode
import app.tick.stock.StockMasterRepository
import org.springframework.stereotype.Service

@Service
class AiSummaryService(
    private val aiServer: AiServerPort,
    private val stockMasterRepository: StockMasterRepository,
) : GetAiSummaryUseCase, EmbedStockNewsUseCase {

    override fun summary(stockCode: StockCode): AiSummaryResult {
        val stockName = stockMasterRepository.findById(stockCode.value).orElseThrow {
            BusinessException(ErrorCode.STOCK_NOT_FOUND)
        }.name
        return aiServer.summarize(stockCode, stockName)
    }

    override fun embed(stockCode: StockCode): EmbedResult =
        aiServer.embed(stockCode)
}
