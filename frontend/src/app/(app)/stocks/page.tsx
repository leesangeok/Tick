import { fetchStocks } from "@/services/stockService";
import { StockTable } from "@/components/stocks/StockTable";

export default async function StocksPage() {
  const stocks = await fetchStocks();
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">종목</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          투자 가능한 종목을 검색하고 시세를 확인하세요.
        </p>
      </div>
      <StockTable stocks={stocks} />
    </div>
  );
}
