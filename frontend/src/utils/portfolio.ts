import {
  ActivityAction,
  ActivityRecord,
  AggregatedHolding,
  PortfolioItem,
} from "../types/portfolio";

const ACTIVITY_STORAGE_KEY = "portfolio-manager-activity-v1";

interface CreateActivityInput {
  action: ActivityAction;
  symbol: string;
  shares: number;
  remainingShares?: number;
}

export function aggregateHoldings(items: PortfolioItem[]): AggregatedHolding[] {
  const grouped = new Map<string, AggregatedHolding>();

  for (const item of items) {
    const normalizedSymbol = item.symbol.trim().toUpperCase();
    const existing = grouped.get(normalizedSymbol);
    if (existing) {
      existing.quantity = Number(
        (existing.quantity + item.quantity).toFixed(8),
      );
      existing.sourceItemIds.push(item.id);
      continue;
    }

    grouped.set(normalizedSymbol, {
      symbol: normalizedSymbol,
      assetType: item.assetType,
      quantity: Number(item.quantity.toFixed(8)),
      sourceItemIds: [item.id],
    });
  }

  return Array.from(grouped.values()).sort(
    (a, b) => b.quantity - a.quantity || a.symbol.localeCompare(b.symbol),
  );
}

export function createActivityRecord(
  input: CreateActivityInput,
): ActivityRecord {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    action: input.action,
    symbol: input.symbol,
    shares: input.shares,
    date: new Date().toISOString(),
    remainingShares: input.remainingShares,
  };
}

export function loadActivities(): ActivityRecord[] {
  if (typeof window === "undefined") {
    return [];
  }

  const raw = window.localStorage.getItem(ACTIVITY_STORAGE_KEY);
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw) as ActivityRecord[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function persistActivities(records: ActivityRecord[]): void {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(ACTIVITY_STORAGE_KEY, JSON.stringify(records));
}
