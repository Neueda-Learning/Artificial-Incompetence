import {
  AggregatedHolding,
  PortfolioItem,
} from "../types/portfolio";

/**
 * 中文：按股票代码合并重复持仓，累计数量并保留对应的数据库记录 ID。
 * English: Groups duplicate symbols, totals their quantities, and keeps the source database IDs.
 */
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
