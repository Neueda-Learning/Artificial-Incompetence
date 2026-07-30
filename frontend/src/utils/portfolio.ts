import {
  AggregatedHolding,
  PortfolioItem,
} from "../types/portfolio";

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
