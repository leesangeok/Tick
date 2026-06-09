import { fetchAccount } from "@/services/accountService";
import { formatKoreanUnit } from "@/lib/format";

export async function AvailableCash() {
  const account = await fetchAccount();
  return (
    <span className="tabular-nums">{formatKoreanUnit(account.cash)}원</span>
  );
}
