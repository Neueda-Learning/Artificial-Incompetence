import { aggregateHoldings } from "./portfolio";
import { PortfolioItem, AssetType } from "../types/portfolio";

describe("aggregateHoldings", () => {
  it("returns empty array for empty input", () => {
    expect(aggregateHoldings([])).toEqual([]);
  });

  it("returns single item unchanged for a single holding", () => {
    const items: PortfolioItem[] = [
      { id: 1, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 10 },
    ];

    const result = aggregateHoldings(items);

    expect(result).toHaveLength(1);
    expect(result[0].symbol).toBe("AAPL");
    expect(result[0].quantity).toBe(10);
    expect(result[0].sourceItemIds).toEqual([1]);
  });

  it("aggregates repeated symbols into one row summing quantities", () => {
    const items: PortfolioItem[] = [
      { id: 1, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 3 },
      { id: 2, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 2 },
    ];

    const result = aggregateHoldings(items);

    expect(result).toHaveLength(1);
    expect(result[0].symbol).toBe("AAPL");
    expect(result[0].quantity).toBe(5);
    expect(result[0].sourceItemIds).toEqual([1, 2]);
  });

  it("normalizes symbols by trimming whitespace", () => {
    const items: PortfolioItem[] = [
      { id: 1, assetType: "STOCK" as AssetType, symbol: " AAPL ", quantity: 5 },
      { id: 2, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 3 },
    ];

    const result = aggregateHoldings(items);

    expect(result).toHaveLength(1);
    expect(result[0].quantity).toBe(8);
  });

  it("normalizes symbols case-insensitively", () => {
    const items: PortfolioItem[] = [
      { id: 1, assetType: "STOCK" as AssetType, symbol: "aapl", quantity: 5 },
      { id: 2, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 3 },
    ];

    const result = aggregateHoldings(items);

    expect(result).toHaveLength(1);
    expect(result[0].quantity).toBe(8);
  });

  it("keeps different symbols separate", () => {
    const items: PortfolioItem[] = [
      { id: 1, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 10 },
      { id: 2, assetType: "STOCK" as AssetType, symbol: "TSLA", quantity: 4 },
    ];

    const result = aggregateHoldings(items);

    expect(result).toHaveLength(2);
  });

  it("sorts by quantity descending", () => {
    const items: PortfolioItem[] = [
      { id: 1, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 5 },
      { id: 2, assetType: "STOCK" as AssetType, symbol: "TSLA", quantity: 15 },
      { id: 3, assetType: "STOCK" as AssetType, symbol: "MSFT", quantity: 10 },
    ];

    const result = aggregateHoldings(items);

    expect(result[0].symbol).toBe("TSLA");
    expect(result[0].quantity).toBe(15);
    expect(result[1].symbol).toBe("MSFT");
    expect(result[1].quantity).toBe(10);
    expect(result[2].symbol).toBe("AAPL");
    expect(result[2].quantity).toBe(5);
  });

  it("sorts by symbol alphabetically when quantities are equal", () => {
    const items: PortfolioItem[] = [
      { id: 1, assetType: "STOCK" as AssetType, symbol: "TSLA", quantity: 10 },
      { id: 2, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 10 },
    ];

    const result = aggregateHoldings(items);

    expect(result[0].symbol).toBe("AAPL");
    expect(result[1].symbol).toBe("TSLA");
  });

  it("handles aggregation of more than two items for the same symbol", () => {
    const items: PortfolioItem[] = [
      { id: 1, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 3 },
      { id: 2, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 2 },
      { id: 3, assetType: "STOCK" as AssetType, symbol: "AAPL", quantity: 5 },
    ];

    const result = aggregateHoldings(items);

    expect(result).toHaveLength(1);
    expect(result[0].quantity).toBe(10);
    expect(result[0].sourceItemIds).toEqual([1, 2, 3]);
  });
});
