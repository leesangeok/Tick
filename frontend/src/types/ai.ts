export type AiReportType = "RISE_REASON" | "FALL_REASON" | "DAILY_SUMMARY";

export type Evidence = {
  title: string;
  source: string;
  publishedAt: string;
  url?: string;
};

export type AiReport = {
  id: string;
  symbol: string;
  stockName: string;
  type: AiReportType;
  summary: string;
  evidences: Evidence[];
  createdAt: string;
};

export type NewsItem = {
  id: string;
  symbol: string;
  title: string;
  source: string;
  summary: string;
  publishedAt: string;
  url?: string;
};

export type Disclosure = {
  id: string;
  symbol: string;
  title: string;
  type: string;
  publishedAt: string;
};
