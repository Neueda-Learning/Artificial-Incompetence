import api from "./api";
import { CreatePortfolioItemRequest, PortfolioItem } from "../types/portfolio";

const USE_MOCK_DATA = process.env.REACT_APP_USE_MOCK_DATA === "true";
const MOCK_ITEMS_STORAGE_KEY = "portfolio-manager-mock-items-v1";
const MOCK_SEEDED_STORAGE_KEY = "portfolio-manager-mock-seeded-v1";

interface RawPortfolioItem {
  id: number;
  assetType: PortfolioItem["assetType"];
  symbol: string;
  quantity: number | string;
}

function normalizeItem(item: RawPortfolioItem): PortfolioItem {
  return {
    id: item.id,
    assetType: item.assetType,
    symbol: item.symbol,
    quantity: Number(item.quantity),
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
