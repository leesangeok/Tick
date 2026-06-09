import type { AiReport } from "@/types/ai";

export const mockAiReports: AiReport[] = [
  {
    id: "ai_001",
    symbol: "005930",
    stockName: "삼성전자",
    type: "RISE_REASON",
    summary:
      "HBM4 양산 일정 단축 보도와 외국인 순매수 확대가 동시에 유입되며 강세. 메모리 가격 인상 기대감이 시장 컨센서스를 상회.",
    evidences: [
      { title: "삼성전자, HBM4 양산 일정 앞당겨", source: "한국경제", publishedAt: "2026-06-08T09:30:00+09:00" },
      { title: "외국인 순매수 상위 종목 1위", source: "한경증권", publishedAt: "2026-06-08T11:00:00+09:00" },
      { title: "DRAM 현물가 주간 +2.1%", source: "DRAMeXchange", publishedAt: "2026-06-08T07:00:00+09:00" },
    ],
    createdAt: "2026-06-08T13:30:00+09:00",
  },
  {
    id: "ai_002",
    symbol: "000660",
    stockName: "SK하이닉스",
    type: "RISE_REASON",
    summary: "엔비디아 차세대 GPU용 HBM 공급 확정 보도로 강세. 2026년 하반기 실적 가시성 확보.",
    evidences: [
      { title: "엔비디아 차세대 GPU용 HBM 공급 확정", source: "전자신문", publishedAt: "2026-06-08T10:15:00+09:00" },
      { title: "외국인·기관 동시 매수", source: "한국거래소", publishedAt: "2026-06-08T11:30:00+09:00" },
    ],
    createdAt: "2026-06-08T13:30:00+09:00",
  },
  {
    id: "ai_003",
    symbol: "035720",
    stockName: "카카오",
    type: "FALL_REASON",
    summary: "광고 단가 하락 지속과 신사업 적자 확대 우려로 약세. 외국인 순매도 5거래일 연속.",
    evidences: [
      { title: "2분기 광고 단가 -7% YoY 추정", source: "이베스트", publishedAt: "2026-06-08T08:00:00+09:00" },
      { title: "외국인 순매도 5거래일 연속", source: "한국거래소", publishedAt: "2026-06-08T11:30:00+09:00" },
    ],
    createdAt: "2026-06-08T13:30:00+09:00",
  },
  {
    id: "ai_daily",
    symbol: "MARKET",
    stockName: "오늘의 시장",
    type: "DAILY_SUMMARY",
    summary:
      "반도체·2차전지 강세, 인터넷·게임 약세. 외국인은 코스피에서 순매수 우위였고, HBM 관련 종목 중심으로 자금이 집중됨. 환율은 안정세.",
    evidences: [
      { title: "코스피 +0.84% 마감", source: "한국거래소", publishedAt: "2026-06-08T15:30:00+09:00" },
      { title: "외국인 순매수 4,820억원", source: "한국거래소", publishedAt: "2026-06-08T15:35:00+09:00" },
    ],
    createdAt: "2026-06-08T15:40:00+09:00",
  },
];
