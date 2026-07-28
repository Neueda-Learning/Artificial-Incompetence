import React from "react";
import { Link } from "react-router-dom";
import {
  ActivityRecord,
  AggregatedHolding,
  AssetPerformance,
  PortfolioPerformance,
  PortfolioValue,
  Transaction,
} from "../../types/portfolio";
import HoldingsMetricsTable from "./HoldingsMetricsTable";
import MarketIndicators from "./MarketIndicators";
import {
  formatDate,
  formatNumber,
  formatPercent,
  formatSignedUsd,
  formatUsd,
} from "../../utils/formatters";

interface DashboardProps {
  holdings: AggregatedHolding[];
  activities: ActivityRecord[];
  transactions: Transaction[];
  portfolioValue: PortfolioValue | null;
  portfolioPerformance: PortfolioPerformance | null;
  performanceBySymbol: Record<string, AssetPerformance>;
  isLoading: boolean;
  isPerformanceLoading: boolean;
  error: string | null;
  performanceError: string | null;
  onRetry: () => void;
  onAddAsset: () => void;
  onRemoveAsset: () => void;
}

function Dashboard({
  holdings,
  activities,
  transactions,
  portfolioValue,
  portfolioPerformance,
  performanceBySymbol,
  isLoading,
  isPerformanceLoading,
  error,
  performanceError,
  onRetry,
  onAddAsset,
  onRemoveAsset,
}: DashboardProps) {
  if (isLoading || isPerformanceLoading) {
  const recentActivities = [
    ...transactions.map((transaction) => ({
      id: `tx-${transaction.id}`,
      action: "Added",
      symbol: transaction.symbol,
      shares: transaction.quantity,
      date: transaction.purchasedAt,
      details: formatUsd(transaction.pricePerUnit),
    })),
    ...activities.map((record) => ({
      id: `local-${record.id}`,
      action: record.action === "ADDED" ? "Added" : "Removed",
      symbol: record.symbol,
      shares: record.shares,
      date: record.date,
      details:
        record.remainingShares !== undefined
          ? `Remaining shares: ${formatNumber(record.remainingShares, 4)}`
          : null,
    })),
  ]
    .sort((a, b) => +new Date(b.date) - +new Date(a.date))
    .slice(0, 6);

  if (isLoading) {
    return (
      <section className="page">
        <div className="skeleton-grid">
          <div className="skeleton-card" />
          <div className="skeleton-card" />
          <div className="skeleton-card" />
          <div className="skeleton-card" />
        </div>
        <div className="skeleton-panel" />
      </section>
    );
  }

  if (error) {
    return (
      <section className="page">
        <div className="state-card">
          <h2>Unable to load dashboard</h2>
          <p>{error}</p>
          <button
            className="button button-secondary"
            type="button"
            onClick={onRetry}
          >
            Retry
          </button>
        </div>
      </section>
    );
  }

  if (holdings.length === 0 && activities.length === 0 && transactions.length === 0) {
    return (
      <section className="page">
        <div className="state-card">
          <h2>Start building your portfolio</h2>
          <p>
            Add your first asset to track portfolio value, allocation,
            performance, and gain or loss.
          </p>
          <button
            type="button"
            className="button button-primary"
            onClick={onAddAsset}
          >
            Add your first asset
          </button>
        </div>
      </section>
    );
  }

  if (holdings.length === 0 && (activities.length > 0 || transactions.length > 0)) {
    return (
      <section className="page">
        <div className="state-card">
          <h2>No current holdings</h2>
          <p>
            All assets have been removed from your portfolio. Your activity
            history is still available.
          </p>
          <div className="inline-actions">
            <button
              type="button"
              className="button button-primary"
              onClick={onAddAsset}
            >
              Add Asset
            </button>
            <Link
              className="button button-secondary"
              to="/holdings?tab=history"
            >
              View History
            </Link>
          </div>
        </div>
      </section>
    );
  }

  const totalShares = holdings.reduce(
    (total, holding) => total + holding.quantity,
    0,
  );

  return (
    <section className="page">
      <div className="metrics-grid">
        <article className="metric-card">
          <h2>Total Portfolio Value</h2>
          <p className="metric-value">
            {formatUsd(portfolioPerformance?.currentValue)}
          </p>
          <p className="metric-subtle">Currency: {portfolioValue?.currency ?? "USD"}</p>
        </article>
        <article className="metric-card">
  <h2>Total Return</h2>
  <p
    className={`metric-value ${
      (portfolioPerformance?.returnPercentage ?? 0) >= 0
        ? "value-positive"
        : "value-negative"
    }`}
  >
    {formatPercent(portfolioPerformance?.returnPercentage)}
  </p>
  <p className="metric-subtle">
    {formatSignedUsd(portfolioPerformance?.unrealizedProfitLoss)}
  </p>
</article>
        <article className="metric-card">
          <h2>Day Change</h2>
          <p className="metric-value">—</p>
          <p className="metric-subtle">
            Intraday change is not supplied by the backend.
          </p>
        </article>
        <article className="metric-card">
          <h2>Total Cost Basis</h2>
          <p className="metric-value">{formatUsd(portfolioPerformance?.totalCost)}</p>
          <p className="metric-subtle">Derived from BUY transactions</p>
        </article>
      </div>

      {performanceError && (
        <div className="banner banner-warning">{performanceError}</div>
      )}

      <MarketIndicators holdings={holdings} performance={portfolioPerformance} />

      <div className="two-column-layout">
        <article className="panel">
          <div className="panel-header">
            <h2>Top Holdings</h2>
            <Link to="/holdings" className="text-link">
              View all holdings
            </Link>
          </div>
          <HoldingsMetricsTable
            holdings={holdings.slice(0, 5)}
            performanceBySymbol={performanceBySymbol}
          />
        </article>

        <article className="panel">
          <div className="panel-header">
            <h2>Recent Activity</h2>
            <button
              className="button button-ghost"
              type="button"
              onClick={onRemoveAsset}
            >
              Remove Asset
            </button>
          </div>
          {recentActivities.length === 0 ? (
            <p className="subtle-text">No activity recorded yet.</p>
          ) : (
            <ul className="activity-list">
              {recentActivities.map((record) => (
                <li key={record.id} className="activity-item">
                  <p>
                    {record.action} {formatNumber(record.shares, 4)} shares of{" "}
                    {record.symbol}
                  </p>
                  <p className="subtle-text">{formatDate(record.date)}</p>
                  {record.details && <p className="subtle-text">{record.details}</p>}
                </li>
              ))}
            </ul>
          )}
        </article>
      </div>

      {portfolioPerformance?.status === "PARTIAL" && (
        <div className="banner banner-warning">
          Some market prices are unavailable: {portfolioPerformance.missingPrices.join(", ")}.
        </div>
      )}

      <div className="summary-line">
        <p>Active symbols: {holdings.length}</p>
        <p>
          Total shares across active holdings: {formatNumber(totalShares, 4)}
        </p>
        <p>Portfolio base currency: {portfolioValue?.currency ?? "USD"}</p>
      </div>
    </section>
  );
}

export default Dashboard;
