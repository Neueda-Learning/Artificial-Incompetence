import api from "./api";
import axios from "axios";
import {
  ActivityRecord,
  AssetPerformance,
  CreatePortfolioItemRequest,
  CreateTransactionRequest,
  HistoricalPerformance,
  HistoricalPerformancePoint,
  PerformanceRange,
  PortfolioItem,
  PortfolioPerformance,
  Transaction,
} from "../types/portfolio";

const USE_MOCK_DATA = process.env.REACT_APP_USE_MOCK_DATA === "true";
const MOCK_ITEMS_STORAGE_KEY = "portfolio-manager-mock-items-v1";
const MOCK_SEEDED_STORAGE_KEY = "portfolio-manager-mock-seeded-v1";

interface RawPortfolioItem {
  id: number;
  assetType: PortfolioItem["assetType"];
  symbol: string;
  quantity: number | string;
}

type NullableNumeric = number | string | null;

interface RawAssetPerformance {
  symbol: string;
  quantity: number | string;
  averageCost: NullableNumeric;
  currentPrice: NullableNumeric;
  costBasis: number | string;
  currentValue: NullableNumeric;
  unrealizedProfitLoss: NullableNumeric;
  returnPercentage: NullableNumeric;
  allocationPercentage: NullableNumeric;
}

interface RawPortfolioPerformance {
  currency: string;
  totalCost: number | string;
  currentValue: number | string;
  unrealizedProfitLoss: number | string;
  returnPercentage: number | string;
  dayChange: NullableNumeric;
  dayChangePercentage: NullableNumeric;
  status: PortfolioPerformance["status"];
  priceUpdatedAt: string | null;
  assets: RawAssetPerformance[];
  missingPrices: string[];
}

interface RawHistoricalPerformancePoint {
  date: string;
  marketValue: NullableNumeric;
  costBasis: NullableNumeric;
  profitLoss: NullableNumeric;
  returnPercentage: NullableNumeric;
}

interface RawHistoricalPerformance {
  currency: string;
  range: PerformanceRange;
  startDate: string;
  endDate: string;
  status: HistoricalPerformance["status"];
  points: RawHistoricalPerformancePoint[];
  assets?: Array<{
    id: string;
    symbol: string;
    assetType: PortfolioItem["assetType"];
    currency: string;
    points: RawHistoricalPerformancePoint[];
  }>;
  missingData: string[];
}

interface RawPortfolioActivity {
  id: number;
  action: ActivityRecord["action"];
  assetType: PortfolioItem["assetType"];
  symbol: string;
  quantity: number | string;
  pricePerUnit: NullableNumeric;
  currency: string | null;
  remainingQuantity: NullableNumeric;
  occurredAt: string;
}

interface ApiErrorResponse {
  message?: string;
  fieldErrors?: Record<string, string>;
}

function requestError(error: unknown, fallback: string): Error {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return error instanceof Error ? error : new Error(fallback);
  }

  const fieldErrors = error.response?.data?.fieldErrors;
  const detail = fieldErrors ? Object.values(fieldErrors)[0] : undefined;
  const message = detail ?? error.response?.data?.message ?? fallback;
  return new Error(message);
}

function nullableNumber(value: NullableNumeric): number | null {
  return value === null ? null : Number(value);
}

function normalizeItem(item: RawPortfolioItem): PortfolioItem {
  return {
    id: item.id,
    assetType: item.assetType,
    symbol: item.symbol,
    quantity: Number(item.quantity),
  };
}

function normalizeAssetPerformance(
  asset: RawAssetPerformance,
): AssetPerformance {
  return {
    symbol: asset.symbol,
    quantity: Number(asset.quantity),
    averageCost: nullableNumber(asset.averageCost),
    currentPrice: nullableNumber(asset.currentPrice),
    costBasis: Number(asset.costBasis),
    currentValue: nullableNumber(asset.currentValue),
    unrealizedProfitLoss: nullableNumber(asset.unrealizedProfitLoss),
    returnPercentage: nullableNumber(asset.returnPercentage),
    allocationPercentage: nullableNumber(asset.allocationPercentage),
  };
}

function normalizeHistoricalPoint(
  point: RawHistoricalPerformancePoint,
): HistoricalPerformancePoint {
  return {
    date: point.date,
    marketValue: nullableNumber(point.marketValue),
    costBasis: nullableNumber(point.costBasis),
    profitLoss: nullableNumber(point.profitLoss),
    returnPercentage: nullableNumber(point.returnPercentage),
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

export async function updatePortfolioItemQuantity(
  id: number,
  quantity: number,
): Promise<PortfolioItem> {
  if (USE_MOCK_DATA) {
    await ensureMockSeeded();
    const currentItems = getStoredMockItems();
    const existing = currentItems.find((item) => item.id === id);
    if (!existing) {
      throw new Error(`Portfolio item ${id} was not found.`);
    }
    const updatedItem = { ...existing, quantity };
    storeMockItems(
      currentItems.map((item) => (item.id === id ? updatedItem : item)),
    );
    return updatedItem;
  }

  const response = await api.put(`/portfolio/items/${id}/quantity`, {
    quantity,
  });
  return normalizeItem(response.data as RawPortfolioItem);
}

export async function createTransaction(
  payload: CreateTransactionRequest,
): Promise<Transaction> {
  if (USE_MOCK_DATA) {
    const item = await createPortfolioItem({
      assetType: payload.assetType,
      symbol: payload.symbol,
      quantity: payload.quantity,
    });
    return {
      id: item.id,
      assetType: payload.assetType,
      symbol: payload.symbol,
      quantity: payload.quantity,
      pricePerUnit: payload.pricePerUnit,
      currency: payload.currency ?? "USD",
      purchasedAt: payload.purchasedAt,
    };
  }

  try {
    const response = await api.post("/transactions", payload);
    const transaction = response.data as Transaction;
    return {
      ...transaction,
      quantity: Number(transaction.quantity),
      pricePerUnit: Number(transaction.pricePerUnit),
    };
  } catch (error) {
    throw requestError(error, "Unable to add this asset.");
  }
}

export async function getTransactions(): Promise<Transaction[]> {
  if (USE_MOCK_DATA) {
    return [];
  }

  try {
    const response = await api.get("/transactions", {
      params: { type: "BUY" },
    });
    return (response.data as Transaction[]).map((transaction) => ({
      ...transaction,
      quantity: Number(transaction.quantity),
      pricePerUnit: Number(transaction.pricePerUnit),
    }));
  } catch (error) {
    throw requestError(error, "Unable to load purchase history.");
  }
}

export async function getPortfolioActivities(): Promise<ActivityRecord[]> {
  if (USE_MOCK_DATA) {
    return [];
  }

  try {
    const response = await api.get("/portfolio/activities");
    return (response.data as RawPortfolioActivity[]).map((activity) => ({
      id: `activity-${activity.id}`,
      action: activity.action,
      symbol: activity.symbol,
      shares: Number(activity.quantity),
      date: activity.occurredAt,
      pricePerUnit:
        activity.pricePerUnit === null
          ? undefined
          : Number(activity.pricePerUnit),
      currency: activity.currency ?? undefined,
      remainingShares:
        activity.remainingQuantity === null
          ? undefined
          : Number(activity.remainingQuantity),
    }));
  } catch (error) {
    throw requestError(error, "Unable to load portfolio activity.");
  }
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

export async function getPortfolioPerformance(): Promise<PortfolioPerformance> {
  const response = await api.get("/portfolio/performance");
  const raw = response.data as RawPortfolioPerformance;

  return {
    currency: raw.currency,
    totalCost: Number(raw.totalCost),
    currentValue: Number(raw.currentValue),
    unrealizedProfitLoss: Number(raw.unrealizedProfitLoss),
    returnPercentage: Number(raw.returnPercentage),
    dayChange: nullableNumber(raw.dayChange),
    dayChangePercentage: nullableNumber(raw.dayChangePercentage),
    status: raw.status,
    priceUpdatedAt: raw.priceUpdatedAt,
    assets: raw.assets.map(normalizeAssetPerformance),
    missingPrices: raw.missingPrices,
  };
}

export async function getHistoricalPerformance(
  range: PerformanceRange,
): Promise<HistoricalPerformance> {
  const response = await api.get("/portfolio/performance/history", {
    params: { range },
  });
  const raw = response.data as RawHistoricalPerformance;

  return {
    currency: raw.currency,
    range: raw.range,
    startDate: raw.startDate,
    endDate: raw.endDate,
    status: raw.status,
    points: raw.points.map(normalizeHistoricalPoint),
    assets: (raw.assets ?? []).map((asset) => ({
      ...asset,
      points: asset.points.map(normalizeHistoricalPoint),
    })),
    missingData: raw.missingData,
  };
}
