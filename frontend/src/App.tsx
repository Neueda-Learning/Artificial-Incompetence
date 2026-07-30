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

/**
 * 中文：应用根组件，集中加载后端数据、维护共享状态、处理增删资产并配置页面路由。
 * English: Root component that loads backend data, owns shared state, handles asset changes, and defines routes.
 */
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

  /**
   * 中文：刷新当前持仓列表，并记录本次成功加载的时间。
   * English: Refreshes current holdings and records the latest successful load time.
   */
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

  /**
   * 中文：刷新实时价格、成本、盈亏及 Day Change 等表现数据。
   * English: Refreshes prices, costs, profit/loss, Day Change, and other performance data.
   */
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

  /**
   * 中文：刷新购买和删除活动；失败时使用空数组，避免阻塞其他模块。
   * English: Refreshes purchase/removal activity and falls back to an empty list without blocking other modules.
   */
  const refreshActivities = useCallback(async () => {
    try {
      setActivities(await getPortfolioActivities());
    } catch {
      setActivities([]);
    }
  }, []);

  /**
   * 中文：并行刷新 Dashboard、Holdings 和 History 共同依赖的三组数据。
   * English: Refreshes the three datasets shared by Dashboard, Holdings, and History in parallel.
   */
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

  /**
   * 中文：把 Add Asset 表单转换为买入请求，提交成功后重新同步全部页面数据。
   * English: Converts the Add Asset form into a purchase request and resynchronizes all page data after success.
   */
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

  /**
   * 中文：部分卖出时更新数量，全部删除时删除持仓，并在完成后刷新全部数据。
   * English: Updates quantity for a partial removal, deletes the holding for a full removal, then refreshes all data.
   */
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

  /**
   * 中文：市场数据超过五分钟未更新时生成提示信息。
   * English: Produces a warning when market data has not been refreshed for five minutes.
   */
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
