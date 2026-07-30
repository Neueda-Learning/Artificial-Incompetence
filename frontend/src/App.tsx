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
  deletePortfolioItem,
  getPortfolioActivities,
  getPortfolioItems,
  getPortfolioPerformance,
  updatePortfolioItemQuantity,
} from "./services/portfolioService";
import {
  ActivityRecord,
  AggregatedHolding,
  PortfolioItem,
  PortfolioPerformance,
} from "./types/portfolio";
import {
  aggregateHoldings,
} from "./utils/portfolio";

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
  const [activities, setActivities] = useState<ActivityRecord[]>([]);

  const refreshItems = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const portfolioItems = await getPortfolioItems();
      setItems(portfolioItems);
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

  const refreshActivities = useCallback(async () => {
    try {
      setActivities(await getPortfolioActivities());
    } catch {
      setActivities([]);
    }
  }, []);

  const refreshAll = useCallback(async () => {
    await Promise.all([
      refreshItems(),
      refreshPerformance(),
      refreshActivities(),
    ]);
  }, [refreshActivities, refreshItems, refreshPerformance]);

  useEffect(() => {
    void refreshAll();
  }, [refreshAll]);

  const holdings = useMemo<AggregatedHolding[]>(
    () => aggregateHoldings(items),
    [items],
  );

  const handleAddAsset = useCallback(
    async (payload: AddAssetPayload) => {
      const currentHolding = holdings.find(
        (holding) => holding.symbol === payload.symbol,
      );
      const transaction = await createTransaction({
        transactionType: "BUY",
        assetType: payload.assetType,
        symbol: payload.symbol,
        quantity: payload.shares,
        pricePerUnit: payload.purchasePrice,
        currency: payload.currency,
        purchasedAt: `${payload.purchaseDate}T00:00:00.000Z`,
      });
      await refreshAll();

      const nextHolding = aggregateHoldings(await getPortfolioItems()).find(
        (holding) => holding.symbol === transaction.symbol,
      );

      const resultLines = [
        `${transaction.symbol} purchase saved successfully.`,
        nextHolding
          ? `Updated shares: ${nextHolding.quantity.toLocaleString()}`
          : `Added shares: ${payload.shares.toLocaleString()}`,
      ];

      if (currentHolding) {
        resultLines.push("The symbol remains a single active holding row.");
      }

      return resultLines.join(" ");
    },
    [holdings, refreshAll],
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

      if (remainingShares > 0) {
        await updatePortfolioItemQuantity(
          targetHolding.sourceItemIds[0],
          remainingShares,
        );
      } else {
        await deletePortfolioItem(targetHolding.sourceItemIds[0]);
      }

      await refreshAll();

      if (remainingShares > 0) {
        return `Removed ${payload.shares.toLocaleString()} shares of ${payload.symbol}. Remaining shares: ${remainingShares.toLocaleString()}. ${payload.symbol} remains active.`;
      }

      return `Removed ${payload.shares.toLocaleString()} shares of ${payload.symbol}. Remaining shares: 0. ${payload.symbol} and its transaction history were deleted.`;
    },
    [holdings, refreshAll],
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
                  performance={performance}
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
                  performance={performance}
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
                  performance={performance}
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
