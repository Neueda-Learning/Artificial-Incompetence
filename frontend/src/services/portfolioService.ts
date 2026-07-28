import api from "./api";
import {
  AssetPerformance,
  CreatePortfolioItemRequest,
  CreateTransactionRequest,
  PortfolioItem,
  PortfolioPerformance,
  PortfolioStatus,
  PortfolioValue,
  PortfolioValueAsset,
  Transaction,
} from "../types/portfolio";

const USE_MOCK_DATA = process.env.REACT_APP_USE_MOCK_DATA === "true";
const MOCK_ITEMS_STORAGE_KEY = "portfolio-manager-mock-items-v1";
const MOCK_SEEDED_STORAGE_KEY = "portfolio-manager-mock-seeded-v1";

interface RawPortfolioItem {
  id: number;
  assetType: PortfolioItem["assetType"];
  symbol: string;
  companyName?: string | null;
  exchange?: string | null;
  quantity: number | string;
  currency?: string | null;
}

interface RawPortfolioValueAsset {
  symbol: string;
  assetType: PortfolioItem["assetType"];
  quantity: number | string;
  currentPrice: number | string | null;
  marketValue: number | string | null;
  currency: string | null;
}

interface RawPortfolioValue {
  currency: string;
  priceUpdatedAt: string | null;
  status: PortfolioStatus;
  assets: RawPortfolioValueAsset[];
  missingPrices: string[];
}

interface RawAssetPerformance {
  symbol: string;
  quantity: number | string;
  averageCost: number | string | null;
  currentPrice: number | string | null;
  costBasis: number | string;
  currentValue: number | string | null;
  unrealizedProfitLoss: number | string | null;
  returnPercentage: number | string | null;
  allocationPercentage: number | string | null;
}

interface RawPortfolioPerformance {
  currency: string;
  totalCost: number | string;
  currentValue: number | string;
  unrealizedProfitLoss: number | string;
  returnPercentage: number | string;
  status: PortfolioPerformance["status"];
  priceUpdatedAt: string | null;
  assets: RawAssetPerformance[];
  missingPrices: string[];
}

interface RawTransaction {
  id: number;
  assetType: PortfolioItem["assetType"];
  symbol: string;
  quantity: number | string;
  pricePerUnit: number | string;
  currency: string;
  purchasedAt: string;
}

function normalizeItem(item: RawPortfolioItem): PortfolioItem {
  return {
    id: item.id,
    assetType: item.assetType,
    symbol: item.symbol,
    quantity: Number(item.quantity),
  };
}

function normalizeOptionalNumber(value: number | string | null): number | null {
  if (value === null) {
    return null;
  }
  return Number(value);
}

function normalizeValueAsset(asset: RawPortfolioValueAsset): PortfolioValueAsset {
  return {
    symbol: asset.symbol,
    assetType: asset.assetType,
    quantity: Number(asset.quantity),
    currentPrice: normalizeOptionalNumber(asset.currentPrice),
    marketValue: normalizeOptionalNumber(asset.marketValue),
    currency: asset.currency,
  };
}

function normalizeAssetPerformance(asset: RawAssetPerformance): AssetPerformance {
  return {
    symbol: asset.symbol,
    quantity: Number(asset.quantity),
    averageCost: normalizeOptionalNumber(asset.averageCost),
    currentPrice: normalizeOptionalNumber(asset.currentPrice),
    costBasis: Number(asset.costBasis),
    currentValue: normalizeOptionalNumber(asset.currentValue),
    unrealizedProfitLoss: normalizeOptionalNumber(asset.unrealizedProfitLoss),
    returnPercentage: normalizeOptionalNumber(asset.returnPercentage),
    allocationPercentage: normalizeOptionalNumber(asset.allocationPercentage),
  };
}

function normalizeTransaction(transaction: RawTransaction): Transaction {
  return {
    id: transaction.id,
    assetType: transaction.assetType,
    symbol: transaction.symbol,
    quantity: Number(transaction.quantity),
    pricePerUnit: Number(transaction.pricePerUnit),
    currency: transaction.currency,
    purchasedAt: transaction.purchasedAt,
  };
}

function getStoredMockItems(): PortfolioItem[] {
  if (typeof window === "undefined") {
    return [];
  }

  const raw = window.localStorage.getItem(MOCK_ITEMS_STORAGE_KEY);
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw) as RawPortfolioItem[];
    return parsed.map(normalizeItem);
  } catch {
    return [];
  }
}

function storeMockItems(items: PortfolioItem[]): void {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(MOCK_ITEMS_STORAGE_KEY, JSON.stringify(items));
}

async function ensureMockSeeded(): Promise<void> {
  if (!USE_MOCK_DATA || typeof window === "undefined") {
    return;
  }

  const alreadySeeded = window.localStorage.getItem(MOCK_SEEDED_STORAGE_KEY);
  if (alreadySeeded) {
    return;
  }

  try {
    const response = await fetch("/mock-data/portfolio-items.json");
    if (!response.ok) {
      throw new Error("Unable to fetch mock data.");
    }
    const rawItems = (await response.json()) as RawPortfolioItem[];
    storeMockItems(rawItems.map(normalizeItem));
  } catch {
    storeMockItems([]);
  }

  window.localStorage.setItem(MOCK_SEEDED_STORAGE_KEY, "true");
}

export async function getPortfolioItems(): Promise<PortfolioItem[]> {
  if (USE_MOCK_DATA) {
    await ensureMockSeeded();
    return getStoredMockItems();
  }

  const response = await api.get("/portfolio/items");
  return (response.data as RawPortfolioItem[]).map(normalizeItem);
}

export async function createPortfolioItem(
  payload: CreatePortfolioItemRequest,
): Promise<PortfolioItem> {
  if (USE_MOCK_DATA) {
    await ensureMockSeeded();
    const currentItems = getStoredMockItems();
    const nextId =
      currentItems.length > 0
        ? Math.max(...currentItems.map((item) => item.id)) + 1
        : 1;
    const newItem: PortfolioItem = {
      id: nextId,
      assetType: payload.assetType,
      symbol: payload.symbol.trim().toUpperCase(),
      quantity: Number(payload.quantity),
    };
    const nextItems = [...currentItems, newItem];
    storeMockItems(nextItems);
    return newItem;
  }

  const response = await api.post("/portfolio/items", payload);
  return normalizeItem(response.data as RawPortfolioItem);
}

export async function deletePortfolioItem(id: number): Promise<void> {
  if (USE_MOCK_DATA) {
    await ensureMockSeeded();
    const nextItems = getStoredMockItems().filter((item) => item.id !== id);
    storeMockItems(nextItems);
    return;
  }

  await api.delete(`/portfolio/items/${id}`);
}

export async function getPortfolioValue(): Promise<PortfolioValue> {
  const response = await api.get("/portfolio/value");
  const raw = response.data as RawPortfolioValue;
  return {
    currency: raw.currency,
    priceUpdatedAt: raw.priceUpdatedAt,
    status: raw.status,
    assets: raw.assets.map(normalizeValueAsset),
    missingPrices: raw.missingPrices,
  };
}

export async function getPortfolioPerformance(): Promise<PortfolioPerformance> {
  const response = await api.get("/portfolio/performance");
  const raw = response.data as RawPortfolioPerformance;
  return {
    currency: raw.currency,
    totalCost: Number(raw.totalCost),
    currentValue: Number(raw.currentValue),
    unrealizedProfitLoss: Number(raw.unrealizedProfitLoss),
    returnPercentage: Number(raw.returnPercentage),
    status: raw.status,
    priceUpdatedAt: raw.priceUpdatedAt,
    assets: raw.assets.map(normalizeAssetPerformance),
    missingPrices: raw.missingPrices,
  };
}

export async function getTransactions(): Promise<Transaction[]> {
  const response = await api.get("/transactions", {
    params: { type: "BUY" },
  });
  return (response.data as RawTransaction[]).map(normalizeTransaction);
}

export async function createTransaction(
  payload: CreateTransactionRequest,
): Promise<Transaction> {
  const response = await api.post("/transactions", payload);
  return normalizeTransaction(response.data as RawTransaction);
}
