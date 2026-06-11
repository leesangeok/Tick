import type { Metadata } from "next";
import { Toaster } from "sonner";
import "./globals.css";
import { ThemeProvider } from "@/components/layout/ThemeProvider";

export const metadata: Metadata = {
  title: "Tick — AI 모의투자 플랫폼",
  description:
    "실시간 시세 기반 AI 모의투자 플랫폼. 가상 자산으로 투자 전략을 실험하고, AI가 주가 변동 이유를 근거와 함께 요약해줍니다.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko" suppressHydrationWarning className="h-full antialiased">
      <body className="min-h-full">
        <ThemeProvider
          attribute="class"
          defaultTheme="dark"
          enableSystem
          disableTransitionOnChange
        >
          {children}
          <Toaster richColors position="top-center" theme="system" />
        </ThemeProvider>
      </body>
    </html>
  );
}
