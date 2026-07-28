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

export type PortfolioStatus = "COMPLETE" | "PARTIAL" | "UNAVAILABLE";

export interface PortfolioValueAsset {
  symbol: string;
  assetType: AssetType;
  quantity: number;
  currentPrice: number | null;
  marketValue: number | null;
  currency: string | null;
}

export interface PortfolioValue {
  currency: string;
  priceUpdatedAt: string | null;
  status: PortfolioStatus;
  assets: PortfolioValueAsset[];
  missingPrices: string[];
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
  status: Exclude<PortfolioStatus, "UNAVAILABLE">;
  priceUpdatedAt: string | null;
  assets: AssetPerformance[];
  missingPrices: string[];
}

export type TransactionType = "BUY";

export interface CreateTransactionRequest {
  transactionType: TransactionType;
  assetType: AssetType;
  symbol: string;
  quantity: number;
  pricePerUnit: number;
  currency: string;
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
