export type AssetType = "STOCK" | "ETF" | "BOND" | "CASH";

export interface PortfolioItem {
  id: number;
  assetType: AssetType;
  symbol: string;
  quantity: number;
}

export interface CreatePortfolioItemRequest {
  assetType: AssetType;
  symbol: string;
  quantity: number;
}

export interface AggregatedHolding {
  symbol: string;
  assetType: AssetType;
  quantity: number;
  sourceItemIds: number[];
}

export type ActivityAction = "ADDED" | "REMOVED";

export interface ActivityRecord {
  id: string;
  action: ActivityAction;
  symbol: string;
  shares: number;
  date: string;
  remainingShares?: number;
}
