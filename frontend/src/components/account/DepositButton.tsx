"use client";

import { useEffect, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Plus, X } from "lucide-react";
import { depositToAccount } from "@/services/accountClient";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

const QUICK = [100_000, 500_000, 1_000_000, 5_000_000];

type Props = {
  variant?: "primary" | "secondary";
  label?: string;
};

export function DepositButton({ variant = "primary", label = "충전" }: Props) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    if (!open) return;
    const handle = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    window.addEventListener("keydown", handle);
    return () => window.removeEventListener("keydown", handle);
  }, [open]);

  const parsed = Number(amount.replace(/[^0-9]/g, ""));
  const canSubmit = parsed > 0 && !isPending;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!canSubmit) return;
    setError(null);
    try {
      await depositToAccount(parsed);
      setAmount("");
      setOpen(false);
      startTransition(() => router.refresh());
    } catch (err) {
      setError(err instanceof Error ? err.message : "충전에 실패했습니다.");
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className={cn(
          "inline-flex items-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium transition-colors",
          variant === "primary"
            ? "bg-primary text-primary-foreground hover:opacity-90"
            : "border border-border text-foreground hover:bg-accent",
        )}
      >
        <Plus className="h-4 w-4" />
        {label}
      </button>

      {open && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 p-4 md:items-center"
          onClick={() => setOpen(false)}
        >
          <form
            onSubmit={handleSubmit}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-sm rounded-lg border border-border bg-card p-5 shadow-2xl"
          >
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-base font-semibold">가상 자산 충전</h2>
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="text-muted-foreground hover:text-foreground"
                aria-label="닫기"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <label className="text-xs text-muted-foreground">충전 금액</label>
            <div className="mt-1 flex items-center gap-2 rounded-md border border-border bg-background px-3 py-2.5">
              <input
                type="text"
                inputMode="numeric"
                value={amount ? formatNumber(parsed) : ""}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="0"
                className="flex-1 bg-transparent text-right text-lg font-semibold tabular-nums outline-none"
                autoFocus
              />
              <span className="text-sm text-muted-foreground">원</span>
            </div>

            <div className="mt-3 grid grid-cols-4 gap-2">
              {QUICK.map((v) => (
                <button
                  key={v}
                  type="button"
                  onClick={() => setAmount(String(parsed + v))}
                  className="rounded-md border border-border py-1.5 text-xs hover:bg-accent"
                >
                  +{v >= 10_000 ? `${v / 10_000}만` : v}
                </button>
              ))}
            </div>

            {error && (
              <p className="mt-3 text-xs text-loss">{error}</p>
            )}

            <button
              type="submit"
              disabled={!canSubmit}
              className="mt-5 w-full rounded-md bg-primary py-2.5 text-sm font-semibold text-primary-foreground transition-opacity hover:opacity-90 disabled:opacity-40"
            >
              {isPending
                ? "충전 중..."
                : canSubmit
                  ? `${formatNumber(parsed)}원 충전`
                  : "금액 입력"}
            </button>
            <p className="mt-2 text-center text-[10px] text-muted-foreground">
              모의투자용 가상 자산입니다. 실제 결제되지 않습니다.
            </p>
          </form>
        </div>
      )}
    </>
  );
}
