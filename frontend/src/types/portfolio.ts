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

export interface UpdatePortfolioItemQuantityRequest {
  quantity: number;
}

export interface CreateTransactionRequest {
  transactionType: "BUY";
  assetType: AssetType;
  symbol: string;
  quantity: number;
  pricePerUnit: number;
  currency?: string;
  purchasedAt: string;
}

export interface Transaction {
  id: number;
  assetType: AssetType;
  symbol: string;
  quantity: number;
  pricePerUnit: number;
  currency: string;
  purchasedAt: string;
}

export interface AggregatedHolding {
  symbol: string;
  assetType: AssetType;
  quantity: number;
  sourceItemIds: number[];
}

export interface AssetPerformance {
  symbol: string;
  quantity: number;
  averageCost: number | null;
  currentPrice: number | null;
  costBasis: number;
  currentValue: number | null;
  unrealizedProfitLoss: number | null;
  returnPercentage: number | null;
  allocationPercentage: number | null;
}

export interface PortfolioPerformance {
  currency: string;
  totalCost: number;
  currentValue: number;
  unrealizedProfitLoss: number;
  returnPercentage: number;
  dayChange: number | null;
  dayChangePercentage: number | null;
  status: "COMPLETE" | "PARTIAL" | "UNAVAILABLE";
  priceUpdatedAt: string | null;
  assets: AssetPerformance[];
  missingPrices: string[];
}

export type PerformanceRange = "1W" | "1M" | "3M" | "1Y" | "ALL";

export interface HistoricalPerformancePoint {
  date: string;
  marketValue: number | null;
  costBasis: number | null;
  profitLoss: number | null;
  returnPercentage: number | null;
}

export interface HistoricalAssetPerformance {
  id: string;
  symbol: string;
  assetType: AssetType;
  currency: string;
  points: HistoricalPerformancePoint[];
}

export interface HistoricalPerformance {
  currency: string;
  range: PerformanceRange;
  startDate: string;
  endDate: string;
  status: "COMPLETE" | "PARTIAL" | "UNAVAILABLE";
  points: HistoricalPerformancePoint[];
  assets: HistoricalAssetPerformance[];
  missingData: string[];
}

export type ActivityAction = "ADDED" | "REMOVED";

export interface ActivityRecord {
  id: string;
  action: ActivityAction;
  symbol: string;
  shares: number;
  date: string;
  remainingShares?: number;
  pricePerUnit?: number;
  currency?: string;
}
