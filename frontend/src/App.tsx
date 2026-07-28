import React, { useCallback, useEffect, useMemo, useState } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Dashboard from "./components/Dashboard/Dashboard";
import Header from "./components/Header/Header";
import Holdings from "./components/Holdings/Holdings";
import Performance from "./components/Performance/Performance";
import AddAssetModal, {
  AddAssetPayload,
} from "./components/Header/AddAssetModal";
import DeleteAssetModal, {
  RemoveAssetPayload,
} from "./components/Header/DeleteAssetModal";
import {
  createTransaction,
  createPortfolioItem,
  deletePortfolioItem,
  getPortfolioPerformance,
  getPortfolioItems,
  getPortfolioValue,
  getTransactions,
} from "./services/portfolioService";
import {
  ActivityRecord,
  AggregatedHolding,
  AssetPerformance,
  PortfolioPerformance,
  PortfolioValue,
  PortfolioItem,
  Transaction,
} from "./types/portfolio";
import {
  aggregateHoldings,
  createActivityRecord,
  loadActivities,
  persistActivities,
} from "./utils/portfolio";

const MAX_ACTIVITY_ITEMS = 200;

function App() {
  const [items, setItems] = useState<PortfolioItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [performance, setPerformance] =
    useState<PortfolioPerformance | null>(null);
  const [isPerformanceLoading, setIsPerformanceLoading] = useState(true);
  const [performanceError, setPerformanceError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<string | null>(null);
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isRemoveOpen, setIsRemoveOpen] = useState(false);
  const [portfolioValue, setPortfolioValue] = useState<PortfolioValue | null>(
    null,
  );
  const [portfolioPerformance, setPortfolioPerformance] =
    useState<PortfolioPerformance | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [activities, setActivities] = useState<ActivityRecord[]>(() =>
    loadActivities(),
  );

  const refreshItems = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [itemsResult, valueResult, performanceResult, transactionsResult] =
        await Promise.allSettled([
          getPortfolioItems(),
          getPortfolioValue(),
          getPortfolioPerformance(),
          getTransactions(),
        ]);

      if (itemsResult.status === "rejected") {
        throw itemsResult.reason;
      }

      const portfolioItems = itemsResult.value;
      setItems(portfolioItems);

      if (valueResult.status === "fulfilled") {
        setPortfolioValue(valueResult.value);
      } else {
        setPortfolioValue(null);
      }

      if (performanceResult.status === "fulfilled") {
        setPortfolioPerformance(performanceResult.value);
      } else {
        setPortfolioPerformance(null);
      }

      if (transactionsResult.status === "fulfilled") {
        setTransactions(transactionsResult.value);
      } else {
        setTransactions([]);
      }

      setLastUpdated(new Date().toISOString());
    } catch (requestError) {
      setError("Unable to load portfolio data. Please retry.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  const refreshPerformance = useCallback(async () => {
    setIsPerformanceLoading(true);
    setPerformanceError(null);
    try {
      const response = await getPortfolioPerformance();
      setPerformance(response);
      if (response.priceUpdatedAt) {
        setLastUpdated(response.priceUpdatedAt);
      }
    } catch (requestError) {
      setPerformanceError(
        "Unable to load current prices and performance. Please retry.",
      );
    } finally {
      setIsPerformanceLoading(false);
    }
  }, []);

  const refreshAll = useCallback(async () => {
    await Promise.all([refreshItems(), refreshPerformance()]);
  }, [refreshItems, refreshPerformance]);

  useEffect(() => {
    void refreshAll();
  }, [refreshAll]);

  useEffect(() => {
    persistActivities(activities);
  }, [activities]);

  const holdings = useMemo<AggregatedHolding[]>(
    () => aggregateHoldings(items),
    [items],
  );

  const performanceBySymbol = useMemo<Record<string, AssetPerformance>>(() => {
    const entries = portfolioPerformance?.assets ?? [];
    return entries.reduce<Record<string, AssetPerformance>>((acc, asset) => {
      acc[asset.symbol.toUpperCase()] = asset;
      return acc;
    }, {});
  }, [portfolioPerformance]);

  const appendActivity = useCallback((record: ActivityRecord) => {
    setActivities((current) =>
      [record, ...current].slice(0, MAX_ACTIVITY_ITEMS),
    );
  }, []);

  const handleAddAsset = useCallback(
    async (payload: AddAssetPayload) => {
      const currentHolding = holdings.find(
        (holding) => holding.symbol === payload.symbol,
      );

      if (payload.purchasePrice !== undefined) {
        await createTransaction({
          transactionType: "BUY",
          assetType: payload.assetType,
          symbol: payload.symbol,
          quantity: payload.shares,
          pricePerUnit: payload.purchasePrice,
          currency: "USD",
          purchasedAt: payload.purchaseDate
            ? new Date(`${payload.purchaseDate}T00:00:00Z`).toISOString()
            : new Date().toISOString(),
        });
      } else {
        await createPortfolioItem({
          assetType: payload.assetType,
          symbol: payload.symbol,
          quantity: payload.shares,
        });
      }

      await refreshItems();

      appendActivity(
        createActivityRecord({
          action: "ADDED",
          symbol: payload.symbol,
          shares: payload.shares,
        }),
      );

      const nextHolding = aggregateHoldings(await getPortfolioItems()).find(
        (holding) => holding.symbol === payload.symbol,
      );

      const resultLines = [
        `${payload.symbol} added successfully.`,
        nextHolding
          ? `Updated shares: ${nextHolding.quantity.toLocaleString()}`
          : `Added shares: ${payload.shares.toLocaleString()}`,
      ];

      if (currentHolding) {
        resultLines.push("The symbol remains a single active holding row.");
      }

      if (payload.purchasePrice !== undefined) {
        resultLines.push("BUY transaction history was recorded in backend.");
      }

      return resultLines.join(" ");
    },
    [appendActivity, holdings, refreshAll],
  );

  const handleRemoveAsset = useCallback(
    async (payload: RemoveAssetPayload) => {
      const targetHolding = holdings.find(
        (holding) => holding.symbol === payload.symbol,
      );
      if (!targetHolding) {
        throw new Error(
          "This asset is no longer active. Refresh and try again.",
        );
      }

      const remainingShares = Number(
        (targetHolding.quantity - payload.shares).toFixed(8),
      );

      for (const sourceId of targetHolding.sourceItemIds) {
        await deletePortfolioItem(sourceId);
      }

      if (remainingShares > 0) {
        await createPortfolioItem({
          assetType: targetHolding.assetType,
          symbol: targetHolding.symbol,
          quantity: remainingShares,
        });
      }

      await refreshAll();

      appendActivity(
        createActivityRecord({
          action: "REMOVED",
          symbol: payload.symbol,
          shares: payload.shares,
          remainingShares,
        }),
      );

      if (remainingShares > 0) {
        return `Removed ${payload.shares.toLocaleString()} shares of ${payload.symbol}. Remaining shares: ${remainingShares.toLocaleString()}. ${payload.symbol} remains active.`;
      }

      return `Removed ${payload.shares.toLocaleString()} shares of ${payload.symbol}. Remaining shares: 0. ${payload.symbol} was removed from current holdings. History remains available.`;
    },
    [appendActivity, holdings, refreshAll],
  );

  const staleWarning = useMemo(() => {
    if (!lastUpdated) {
      return null;
    }
    const ageMs = Date.now() - new Date(lastUpdated).getTime();
    return ageMs > 5 * 60 * 1000
      ? "Market data may be stale. Last refresh was over 5 minutes ago."
      : null;
  }, [lastUpdated]);

  return (
    <BrowserRouter>
      <div className="app-shell">
        <Header
          onAddAsset={() => setIsAddOpen(true)}
          onRemoveAsset={() => setIsRemoveOpen(true)}
          lastUpdated={lastUpdated}
          staleWarning={staleWarning}
        />
        <main className="app-main" aria-live="polite">
          <Routes>
            <Route
              path="/dashboard"
              element={
                <Dashboard
                  holdings={holdings}
                  activities={activities}
                  transactions={transactions}
                  portfolioValue={portfolioValue}
                  portfolioPerformance={portfolioPerformance}
                  performanceBySymbol={performanceBySymbol}
                  isLoading={isLoading}
                  isPerformanceLoading={isPerformanceLoading}
                  error={error}
                  performanceError={performanceError}
                  onRetry={refreshAll}
                  onAddAsset={() => setIsAddOpen(true)}
                  onRemoveAsset={() => setIsRemoveOpen(true)}
                />
              }
            />
            <Route
              path="/holdings"
              element={
                <Holdings
                  holdings={holdings}
                  activities={activities}
                  transactions={transactions}
                  performanceBySymbol={performanceBySymbol}
                  isLoading={isLoading}
                  isPerformanceLoading={isPerformanceLoading}
                  error={error}
                  performanceError={performanceError}
                  onRetry={refreshAll}
                />
              }
            />
            <Route
              path="/performance"
              element={
                <Performance
                  holdings={holdings}
                  portfolioPerformance={portfolioPerformance}
                  performanceBySymbol={performanceBySymbol}
                  isLoading={isLoading}
                  isPerformanceLoading={isPerformanceLoading}
                  error={error}
                  performanceError={performanceError}
                  onRetry={refreshAll}
                />
              }
            />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </main>
        <AddAssetModal
          isOpen={isAddOpen}
          onClose={() => setIsAddOpen(false)}
          onSubmit={handleAddAsset}
          holdings={holdings}
        />
        <DeleteAssetModal
          isOpen={isRemoveOpen}
          onClose={() => setIsRemoveOpen(false)}
          onSubmit={handleRemoveAsset}
          holdings={holdings}
        />
      </div>
    </BrowserRouter>
  );
}

export default App;
