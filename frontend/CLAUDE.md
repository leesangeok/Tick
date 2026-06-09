# CLAUDE.md

@AGENTS.md

## 프로젝트 개요

이 프로젝트는 **실시간 주식 시세 기반 AI 모의투자 플랫폼**입니다.

사용자는 가상 자산으로 주식을 매수/매도하고, 보유 자산과 수익률을 확인할 수 있습니다. 또한 AI가 뉴스, 공시, 가격 변동 데이터를 기반으로 종목의 상승/하락 이유를 요약해줍니다.

현재 단계에서는 **백엔드 연동 없이 프론트엔드 화면과 디자인을 먼저 구현**합니다.

---

## 현재 목표

먼저 Next.js 기반 프론트엔드 프로토타입을 완성합니다.

우선순위:

1. 전체 서비스 디자인 방향 정립
2. 주요 페이지 라우팅 구성
3. 대시보드 UI 구현
4. 종목 목록 및 종목 상세 화면 구현
5. 매수/매도 주문 패널 UI 구현
6. 포트폴리오 화면 구현
7. AI 분석 리포트 화면 구현
8. Mock Data 기반으로 실제 서비스처럼 동작하게 구성
9. 이후 Spring Boot Kotlin 백엔드와 연동하기 쉬운 구조로 설계

백엔드 코드는 아직 구현하지 않습니다.

---

## 기술 스택

### Frontend

* Next.js (App Router)
* TypeScript (`strict: true`, `noUncheckedIndexedAccess: true`)
* Tailwind CSS
* shadcn/ui
* Zustand (클라이언트 상태)
* TanStack Query (서버 상태 / mock service 호출)
* Recharts 또는 Lightweight Charts (시세 차트)
* Lucide React (아이콘)
* **Pretendard Variable** (한글/숫자 가독성)
* **next-themes** (다크모드)
* **react-hook-form + zod** (주문 폼 검증)
* **sonner** (토스트)
* **framer-motion** 또는 **react-countup** (가격 카운트업/플래시)
* **date-fns + ko locale** (날짜/상대시간)
* **react-hotkeys-hook** (키보드 단축키)
* **@faker-js/faker** (mock data 양산)

### 디자인 검증 도구

* **Playwright + Playwright MCP** — Claude가 직접 스크린샷 찍어 디자인 반복

### Backend (예정 — 현재 미구현)

추후 연동을 고려한 구조만 잡습니다.

* Kotlin
* Spring Boot 3
* Spring Security
* JPA
* PostgreSQL
* Redis
* Kafka
* WebSocket 또는 SSE
* pgvector 기반 RAG

---

## 디자인 방향

전체 디자인은 **현대적인 금융/투자 대시보드** 느낌으로 구성합니다.

참고 분위기:

* 토스증권
* Robinhood
* TradingView
* Upbit 스타일 자산 대시보드
* Modern SaaS Dashboard

원칙:

* 다크 모드 우선
* 전문적인 금융 서비스 느낌
* 너무 화려하거나 장난스러운 UI 지양
* 카드 기반 레이아웃
* 여백을 충분히 사용
* 정보 계층이 명확
* **상승 = 빨강 / 하락 = 파랑 (한국 증권 관행)**
* 수치는 한눈에 잘 보여야 함
* 데스크톱 우선 구현 후 모바일 반응형 대응

---

## 색상 규칙 (한국 증권 관행)

| 상태 | 색상 | Tailwind |
|------|------|----------|
| 상승 / 이익 | 빨강 | `text-red-500`, `bg-red-500/10` |
| 하락 / 손실 | 파랑 | `text-blue-500`, `bg-blue-500/10` |
| 보합 / 변화 없음 | 회색 | `text-muted-foreground` |
| 보조 텍스트 | 회색 | `text-muted-foreground` |
| 카드 배경 | — | `bg-card` |
| 테두리 | — | `border-border` |
| 강조 버튼 | — | `bg-primary text-primary-foreground` |

### 색상 외 시그널 (필수)

색상에만 의존하지 말고 **부호와 기호를 항상 함께 표시**합니다.

```txt
상승: ▲ +3.42%
하락: ▼ -1.20%
보합: — 0.00%
금액: +15,000원 / -15,000원
```

이유:

* 색맹/저시력 사용자 접근성
* 흑백 인쇄/스크린샷에서도 의미 전달
* ARIA label에 "상승 3.42 퍼센트" 같은 텍스트 동시 노출

### CSS 토큰

shadcn `--destructive` 와 상승 빨강이 충돌하지 않도록 별도 토큰 사용:

```css
:root {
  --gain: 0 84% 60%;   /* 상승 빨강 */
  --loss: 217 91% 60%; /* 하락 파랑 */
}
```

`tailwind.config.ts`에서 `colors.gain`, `colors.loss` 로 매핑.

---

## 전체 레이아웃

기본 구조:

```txt
좌측 사이드바 + 상단 네비게이션 + 메인 콘텐츠 영역
```

데스크톱:

```txt
[Sidebar] [Main Content]
```

모바일:

```txt
[Top Navigation]
[Main Content]
[Bottom Navigation]
```

공통 구성 요소:

* 좌측 사이드바
* 상단 네비게이션
* 페이지 헤더
* 카드형 섹션
* 모바일 하단 네비게이션
* 사용자 프로필 영역
* 다크 모드 / 라이트 모드 토글 (next-themes)

---

## 주요 페이지

### 1. 랜딩 페이지 — `/`

서비스를 소개하고 사용자가 대시보드로 진입할 수 있게 합니다.

포함:

* Hero Section
* 서비스 소개
* 핵심 기능 소개
* 실시간 모의투자 소개
* AI 주가 분석 기능 소개
* CTA 버튼

메인 카피 예시:

```txt
실시간 시세 기반 AI 모의투자 플랫폼
가상 자산으로 투자 전략을 실험하고, AI가 주가 변동 이유를 요약해줍니다.
```

CTA: `모의투자 시작하기`

---

### 2. 대시보드 페이지 — `/dashboard`

사용자의 전체 자산 현황을 한눈에 보여줍니다.

포함:

* 총 자산
* 보유 현금
* 주식 평가 금액
* 오늘 손익
* 전체 수익률
* 포트폴리오 차트
* 보유 종목 리스트
* 최근 거래 내역
* AI 포트폴리오 요약

---

### 3. 종목 목록 페이지 — `/stocks`

투자 가능한 종목 검색/확인.

포함:

* 종목 검색 입력창 (`/` 단축키로 포커스)
* 시장 필터 (전체 / KOSPI / KOSDAQ)
* 업종 필터
* 종목 테이블
* 관심 종목 버튼
* 종목 상세 페이지 이동 링크

테이블 컬럼:

```txt
종목명 | 현재가 | 전일 대비 | 등락률 | 거래량 | 시장 | 관심
```

---

### 4. 종목 상세 페이지 — `/stocks/[symbol]`

특정 종목의 시세, 차트, 주문, 뉴스, AI 분석.

상단:

```txt
종목명 / 종목코드 / 현재가 / 등락률 / 관심 종목 버튼
```

중앙:

```txt
실시간 차트 + 주문 패널
```

하단:

```txt
AI 상승/하락 이유 요약
관련 뉴스
관련 공시
최근 가격 이벤트
```

주문 패널 UI:

```txt
매수 / 매도 탭 (b / s 단축키)
시장가 / 지정가 선택
수량 입력
가격 입력
주문 예상 금액
주문 버튼
```

실제 주문 API는 아직 연결하지 않습니다. 버튼 클릭 시:

1. 확인 다이얼로그(shadcn `Dialog`) — 예상 주문 금액 재노출
2. 확인 → `sonner` 토스트로 모의 주문 완료 메시지

---

### 5. 포트폴리오 페이지 — `/portfolio`

보유 종목과 수익률 상세.

포함:

* 총 자산 요약
* 보유 현금
* 보유 종목 목록
* 종목별 평가 손익 / 수익률
* 자산 비중 차트 (파이)
* 거래 내역
* AI 포트폴리오 리포트

---

### 6. 주문 내역 페이지 — `/orders`

주문 및 체결 내역.

필터: 전체 / 매수 / 매도 / 대기 / 체결 / 취소

주문 상태: `대기 / 체결 / 취소 / 거절`

---

### 7. AI 리포트 페이지 — `/ai-report`

AI 생성 투자 요약.

카드:

* 오늘의 시장 요약
* 내 포트폴리오 요약
* 상승 이유 요약
* 하락 이유 요약
* 뉴스 요약
* 공시 요약

각 요약에는 근거 목록을 포함합니다.

---

## 주요 컴포넌트

### Layout

```txt
AppSidebar
TopNavigation
MobileNavigation
PageHeader
SectionHeader
DashboardLayout
ThemeProvider (next-themes)
```

### Dashboard

```txt
AssetSummaryCard
ProfitRateCard
PortfolioChart
HoldingList
RecentTradeList
AiSummaryCard
```

### Stock

```txt
StockSearchBar
StockTable
StockPriceCard
StockChart
StockDetailHeader
StockNewsList
DisclosureList
FavoriteButton
```

### Trading

```txt
OrderPanel
BuyOrderForm
SellOrderForm
OrderTypeTabs
OrderSummary
OrderConfirmDialog
TradeHistoryTable
```

### AI

```txt
AiReasonSummary
AiEvidenceList
AiReportCard
AiPortfolioSummary
NewsSummaryCard
DisclosureSummaryCard
```

### 공통 상태

```txt
EmptyState        — 데이터 없음
Skeleton          — 로딩 (shadcn)
ErrorBoundary     — Next.js error.tsx
PriceTicker       — 가격 카운트업/플래시 (framer-motion)
```

---

## Mock Data 우선 구현

실제 API는 연결하지 않습니다. Mock Data로 실제 서비스처럼 보이게 구현.

위치:

```txt
src/mocks/
  stocks.ts        — 종목 마스터 (정적)
  priceSeries.ts   — 시계열 가격 (차트용, 30~60일 일봉)
  portfolio.ts     — 보유 / 현금
  orders.ts        — 주문 내역
  news.ts          — 종목별 뉴스 5~10건
  disclosures.ts   — 공시 더미
  aiReports.ts     — 종목별 2~3건
```

### 양산 가이드

* 종목명/시장/업종은 **정적**으로 유지 (실제 한국 종목 사용).
* 시계열 가격, 뉴스 본문, 거래 시각 등은 `@faker-js/faker` + `ko` locale로 양산.
* **시계열은 반드시 만들어야 차트가 의미 있게 보임** — 종목당 최소 30~60 포인트.
* 가격은 랜덤 워크 (이전 가격 ± 0.1~2%) 로 자연스럽게.

### 한국 주식 예시

```txt
삼성전자 / SK하이닉스 / NAVER / 카카오 / 현대차
LG에너지솔루션 / 셀트리온 / 한화에어로스페이스
두산에너빌리티 / POSCO홀딩스
```

### 실시간 느낌 시뮬레이션

* TanStack Query `refetchInterval: 2000` 또는 `setInterval`로 mock 가격을 1~3초마다 ±0.05~0.2% 흔들기.
* 가격 변경 시 `framer-motion`으로 카운트업 + 빨강/파랑 잠깐 플래시.
* 추후 백엔드 붙일 때 `services/` 내부만 폴링 → WebSocket으로 교체.

---

## Mock Data 타입

### Stock

```ts
export type Stock = {
  symbol: string
  name: string
  market: 'KOSPI' | 'KOSDAQ'
  sector: string
  currentPrice: number
  changeAmount: number
  changeRate: number
  volume: number
  isFavorite: boolean
}
```

### PricePoint

```ts
export type PricePoint = {
  timestamp: string  // ISO
  open: number
  high: number
  low: number
  close: number
  volume: number
}
```

### Holding

```ts
export type Holding = {
  symbol: string
  name: string
  quantity: number
  averagePrice: number
  currentPrice: number
  evaluationAmount: number
  profitLoss: number
  profitRate: number
}
```

### Order

```ts
export type Order = {
  id: string
  symbol: string
  stockName: string
  side: 'BUY' | 'SELL'
  orderType: 'MARKET' | 'LIMIT'
  quantity: number
  price: number
  status: 'PENDING' | 'FILLED' | 'CANCELED' | 'REJECTED'
  createdAt: string
}
```

### AI Report

```ts
export type AiReport = {
  id: string
  symbol: string
  stockName: string
  type: 'RISE_REASON' | 'FALL_REASON' | 'DAILY_SUMMARY'
  summary: string
  evidences: {
    title: string
    source: string
    publishedAt: string
    url?: string
  }[]
  createdAt: string
}
```

---

## UI 요구사항

* Tailwind CSS 사용.
* shadcn/ui 컴포넌트 적극 활용.
* 카드 기반 대시보드 레이아웃.
* 테이블, 차트, 카드, 탭 적절히 조합.
* 일관된 spacing 유지.
* 데스크톱과 모바일 모두 자연스럽게.
* 로딩: `Skeleton` (shadcn).
* 데이터 없음: `EmptyState` 컴포넌트.
* 에러: Next.js `error.tsx` + `ErrorBoundary`.
* `prefers-reduced-motion` 환경에선 애니메이션 비활성화.

---

## 숫자 포맷 유틸

위치: `src/lib/format.ts`

```ts
formatCurrency(value: number): string         // 12,500원
formatNumber(value: number): string           // 1,250,000
formatPercent(value: number): string          // 3.42%
formatSignedCurrency(value: number): string   // +15,000원 / -15,000원
formatSignedPercent(value: number): string    // +3.42% / -1.20%
formatKoreanUnit(value: number): string       // 12.3억 / 3,500만 / 1,234원
formatRelativeTime(date: Date | string): string  // "5분 전" (date-fns + ko)
```

표시 예시:

```txt
12,500원
+3.42%
-15,000원
1,250,000원
12.3억
3,500만
5분 전
```

규칙:

* 대시보드 카드처럼 한눈에 보여야 하는 곳 → `formatKoreanUnit`
* 주문 입력처럼 정확한 자릿수가 필요한 곳 → `formatCurrency`

---

## 상태 관리

Zustand.

```txt
src/stores/useWatchlistStore.ts   — 관심 종목 추가/삭제
src/stores/useOrderStore.ts       — 주문 입력 상태, 매수/매도 탭
src/stores/useUiStore.ts          — 사이드바, 모바일 메뉴
```

서버 상태(mock service 결과)는 TanStack Query로 관리. Zustand에 중복 저장 금지.

---

## 주문 폼 검증

`react-hook-form` + `zod`.

```ts
const orderSchema = z.object({
  quantity: z.number().int().min(1, '수량은 1주 이상'),
  price: z.number().positive('가격은 0보다 커야 합니다'),
}).refine(({ quantity, price }) => quantity * price <= cash, {
  message: '보유 현금이 부족합니다',
  path: ['quantity'],
})
```

주문 흐름:

1. 폼 입력 → 실시간 검증
2. 주문 버튼 클릭 → `OrderConfirmDialog` 오픈 (예상 금액 재노출)
3. 확인 → `sonner` 토스트로 모의 주문 완료

---

## API 연동 대비 구조

지금은 mock data, 나중에 API 연동이 쉬워야 합니다.

```txt
src/services/
  stockService.ts
  portfolioService.ts
  orderService.ts
  aiReportService.ts
```

현재 구현:

```ts
export async function getStocks(): Promise<Stock[]> {
  return mockStocks
}
```

나중에 fetch/axios로 한 줄 교체:

```ts
export async function getStocks(): Promise<Stock[]> {
  const res = await fetch('/api/stocks')
  return res.json()
}
```

호출부(TanStack Query)는 그대로 둡니다.

---

## 라우팅 구조

```txt
src/app/
  layout.tsx
  page.tsx                    — 랜딩
  dashboard/page.tsx
  stocks/
    page.tsx
    [symbol]/page.tsx
  portfolio/page.tsx
  orders/page.tsx
  ai-report/page.tsx
```

---

## 추천 폴더 구조

```txt
src/
  app/
    page.tsx
    layout.tsx
    dashboard/page.tsx
    stocks/
      page.tsx
      [symbol]/page.tsx
    portfolio/page.tsx
    orders/page.tsx
    ai-report/page.tsx

  components/
    layout/
      AppSidebar.tsx
      TopNavigation.tsx
      MobileNavigation.tsx
      PageHeader.tsx
      ThemeProvider.tsx
    dashboard/
      AssetSummaryCard.tsx
      PortfolioChart.tsx
      HoldingList.tsx
      RecentTradeList.tsx
      AiSummaryCard.tsx
    stocks/
      StockSearchBar.tsx
      StockTable.tsx
      StockChart.tsx
      StockDetailHeader.tsx
      StockNewsList.tsx
      DisclosureList.tsx
    trading/
      OrderPanel.tsx
      BuyOrderForm.tsx
      SellOrderForm.tsx
      OrderSummary.tsx
      OrderConfirmDialog.tsx
      TradeHistoryTable.tsx
    ai/
      AiReasonSummary.tsx
      AiEvidenceList.tsx
      AiReportCard.tsx
      AiPortfolioSummary.tsx
    common/
      EmptyState.tsx
      PriceTicker.tsx
    ui/
      <shadcn components>

  mocks/
    stocks.ts
    priceSeries.ts
    portfolio.ts
    orders.ts
    news.ts
    disclosures.ts
    aiReports.ts

  services/
    stockService.ts
    portfolioService.ts
    orderService.ts
    aiReportService.ts

  stores/
    useWatchlistStore.ts
    useOrderStore.ts
    useUiStore.ts

  lib/
    format.ts
    utils.ts

  types/
    stock.ts
    portfolio.ts
    order.ts
    ai.ts

  styles/
    pretendard.css   — Pretendard variable woff2 self-host
```

---

## 화면별 세부 요구사항

### Dashboard

상단: 자산 요약 카드 5개

```txt
총 자산 | 보유 현금 | 평가 금액 | 오늘 손익 | 총 수익률
```

중앙: 포트폴리오 차트 + 보유 종목 리스트

하단: 최근 거래 내역 + AI 요약 카드

AI 요약 예시:

```txt
오늘 포트폴리오는 반도체 종목 강세 영향으로 전체 수익률이 상승했습니다.
특히 SK하이닉스와 삼성전자가 HBM 수요 기대감으로 상승하면서
평가금액 증가에 기여했습니다.
```

---

### Stocks

테이블 중심. 검색/필터 자연스럽게.

필터: `전체 / KOSPI / KOSDAQ / 관심 종목 / 상승 / 하락`

행 클릭 → 상세 페이지.

---

### Stock Detail

가장 중요한 페이지.

차트:

* 라인 또는 캔들 (Recharts / Lightweight Charts)
* hover tooltip + 십자선 + 가격 라벨

주문 패널은 위 "주문 폼 검증" 섹션 참고.

---

### Portfolio

보유 종목 중심:

```txt
보유 수량 | 평균 단가 | 현재가 | 평가 금액 | 평가 손익 | 수익률 | 자산 비중
```

자산 비중 파이 차트 포함.

---

### Orders

테이블. 필터: `전체 / 매수 / 매도 / 대기 / 체결 / 취소`

---

### AI Report

카드 기반. 각 카드는 요약 + 근거 목록.

근거 예시:

```txt
- 09:30 뉴스: 반도체 업황 개선 기대
- 10:15 뉴스: 외국인 순매수 확대
- 11:00 공시: 신규 공급계약 체결
```

---

## 차트 요구사항

초기에는 실시세가 아니어도 됨. Mock 시계열 기반.

* 종목 가격 라인 차트
* 포트폴리오 자산 변화 차트
* 자산 비중 파이 차트
* 수익률 변화 차트

Recharts 우선. 캔들이 필요하면 Lightweight Charts.

---

## 키보드 단축키 (UX 디테일)

`react-hotkeys-hook` 사용.

| 키 | 동작 |
|----|------|
| `/` | 종목 검색창 포커스 |
| `b` | 매수 탭으로 전환 (종목 상세에서) |
| `s` | 매도 탭으로 전환 (종목 상세에서) |
| `Esc` | 모달/다이얼로그 닫기 |
| `g d` | 대시보드로 이동 |
| `g s` | 종목 목록으로 이동 |
| `g p` | 포트폴리오로 이동 |

---

## 접근성 / UX

* 버튼에 명확한 텍스트.
* 숫자 색상만으로 의미 전달 금지 — `+`, `-`, `▲`, `▼` 병기.
* 테이블은 모바일에서 카드형으로 전환 고려.
* 클릭 가능한 영역은 hover 효과.
* 주요 액션 버튼은 명확히 구분.
* 주문 버튼 클릭 전 예상 금액 표시 + 확인 다이얼로그.
* ARIA label: 수치에 텍스트 동시 노출 ("상승 3.42 퍼센트").
* `prefers-reduced-motion` 대응.

---

## 디자인 검증 도구 — Playwright

Claude가 직접 스크린샷 찍어 디자인을 시각적으로 반복하기 위한 설정.

### 설치

```bash
npm i -D @playwright/test
npx playwright install chromium
claude mcp add playwright -- npx -y @playwright/mcp@latest
```

### 활용

* 페이지 스크린샷 → 시각 검증 / 반복
* 데스크톱(1440px) ↔ 모바일(375px) viewport 비교
* 다크/라이트 토글 결과 캡처
* 콘솔 에러/네트워크 실패 자동 캐치
* "이 카드 여백을 더 줘봐" → 스크린샷 → 비교 루프

### 대안 / 보조

* **Chrome DevTools MCP**: 콘솔/네트워크 디버깅에 강함. Playwright와 병행 가능.

E2E 테스트는 백엔드 연동 이후 본격적으로.

---

## 개발 도구 설정

* TypeScript `strict: true`, `noUncheckedIndexedAccess: true`
* ESLint (`eslint-config-next`)
* Prettier + `prettier-plugin-tailwindcss` (클래스 자동 정렬)
* Husky + lint-staged pre-commit: `eslint --fix`, `prettier --write`
* `.editorconfig`로 LF/UTF-8/2 spaces 통일

---

## 구현 시 주의사항

* 백엔드 코드는 만들지 않습니다.
* 실제 주식 API 연동도 아직 하지 않습니다.
* 모든 데이터는 mock data로 처리합니다.
* 컴포넌트는 재사용 가능하게 분리합니다.
* 페이지 파일에 모든 UI를 몰아넣지 않습니다.
* 타입을 명확히 정의합니다.
* `any` 사용을 피합니다.
* UI 먼저 완성하는 것이 목표입니다.
* 디자인 완성도가 중요합니다.
* 나중에 API 연동하기 쉽도록 `services/` 계층을 분리합니다.

---

## 개발 순서

1. Next.js 프로젝트 부트스트랩 (App Router, TS, Tailwind)
2. shadcn/ui 초기화
3. Pretendard 폰트 self-host
4. ThemeProvider(next-themes) + dark 우선 설정
5. CSS 토큰(`--gain`, `--loss`) + Tailwind 매핑
6. 공통 레이아웃 (Sidebar, TopNav, MobileNav)
7. Mock Data 및 Type 정의 (`stocks`, `priceSeries`)
8. 숫자 포맷 유틸 (`format.ts`)
9. 랜딩 페이지
10. 대시보드 페이지
11. 종목 목록 페이지
12. 종목 상세 페이지 (차트 포함)
13. 주문 패널 (react-hook-form + zod + Dialog + sonner)
14. 포트폴리오 페이지
15. 주문 내역 페이지
16. AI 리포트 페이지
17. Playwright MCP 연결 → 스크린샷 기반 시각 반복
18. 반응형 정리
19. 컴포넌트 리팩토링

---

## 코드 스타일

* TypeScript 함수형 컴포넌트.
* Props 타입 명확히.
* 재사용 UI는 `components/`로.
* 비즈니스 타입은 `types/`.
* 포맷팅 함수는 `lib/`.
* mock data는 `mocks/`.
* API 역할 함수는 `services/`.
* 파일 네이밍: 컴포넌트 PascalCase, util/훅 camelCase.

---

## 예시 문구

서비스 소개:

```txt
가상 자산으로 실시간 모의투자를 경험하고,
AI가 주가 변동의 이유를 근거와 함께 요약해줍니다.
```

AI 분석:

```txt
AI는 최근 뉴스, 공시, 가격 변동 데이터를 기반으로
종목의 상승 또는 하락 원인을 요약합니다.
```

대시보드 요약:

```txt
오늘 내 포트폴리오는 반도체 업종 강세 영향으로
전일 대비 상승했습니다.
```

빈 상태:

```txt
아직 보유 중인 종목이 없습니다.
관심 있는 종목을 찾아 첫 모의투자를 시작해보세요.
```

주문 완료:

```txt
모의 주문이 접수되었습니다.
실제 체결 로직은 백엔드 연동 후 적용됩니다.
```

---

## 의도적으로 미포함 (현 단계 범위 외)

* **Storybook** — 프로토타입에는 오버킬. 컴포넌트 안정화 후 검토.
* **i18n** — 한국어 단일이면 불필요. 추후 영어 지원 시 `next-intl`.
* **단위 테스트 / 본격 E2E** — Playwright는 시각 검증 위주로만 사용. 본격 테스트는 백엔드 연동 이후.
* **PWA / 푸시 알림** — 범위 외.
* **상태 머신(XState)** — 주문 플로우가 단순해서 Zustand로 충분.
* **실제 시세 API (한투 KIS 등)** — 백엔드 도입 후.

---

## 최종 목표

실제 백엔드가 없어도 서비스가 완성된 것처럼 보이는 프론트엔드 프로토타입.

사용자 흐름:

```txt
랜딩 페이지
→ 대시보드
→ 종목 목록
→ 종목 상세
→ 매수/매도 주문
→ 포트폴리오 확인
→ AI 리포트 확인
```

포트폴리오에 넣었을 때 완성도 있는 금융 서비스처럼 보여야 합니다.
