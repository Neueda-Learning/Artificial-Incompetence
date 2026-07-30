import React from "react";
import { Link } from "react-router-dom";
import {
  ActivityRecord,
  AggregatedHolding,
  PortfolioPerformance,
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
  performance: PortfolioPerformance | null;
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
  performance,
  isLoading,
  isPerformanceLoading,
  error,
  performanceError,
  onRetry,
  onAddAsset,
  onRemoveAsset,
}: DashboardProps) {
  if (isLoading || isPerformanceLoading) {
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

  if (holdings.length === 0 && activities.length === 0) {
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

  if (holdings.length === 0 && activities.length > 0) {
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
          <p className="metric-value">{formatUsd(performance?.currentValue)}</p>
          <p className="metric-subtle">
            Current value in {performance?.currency ?? "USD"}.
          </p>
        </article>
        <article className="metric-card">
          <h2>Total Return</h2>
          <p
            className={`metric-value ${
              (performance?.returnPercentage ?? 0) >= 0
                ? "value-positive"
                : "value-negative"
            }`}
          >
            {formatPercent(performance?.returnPercentage)}
          </p>
          <p className="metric-subtle">
            Since recorded purchases.
          </p>
        </article>
        <article className="metric-card">
          <h2>Day Change</h2>
          <p
            className={`metric-value ${
              performance?.dayChange == null
                ? ""
                : performance.dayChange >= 0
                  ? "value-positive"
                  : "value-negative"
            }`}
          >
            {formatSignedUsd(performance?.dayChange)}
          </p>
          <p className="metric-subtle">
            {performance?.dayChangePercentage == null
              ? "Previous-close data is currently unavailable."
              : `${formatPercent(
                  performance.dayChangePercentage,
                )} since the previous market close.`}
          </p>
        </article>
        <article className="metric-card">
          <h2>Total Cost Basis</h2>
          <p className="metric-value">{formatUsd(performance?.totalCost)}</p>
          <p className="metric-subtle">
            Unrealized P/L:{" "}
            {formatSignedUsd(performance?.unrealizedProfitLoss)}
          </p>
        </article>
      </div>

      {performanceError && (
        <div className="banner banner-warning">{performanceError}</div>
      )}

      <MarketIndicators holdings={holdings} performance={performance} />

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
            performance={performance}
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
          {activities.length === 0 ? (
            <p className="subtle-text">No activity recorded yet.</p>
          ) : (
            <ul className="activity-list">
              {activities.slice(0, 6).map((record) => (
                <li key={record.id} className="activity-item">
                  <p>
                    {record.action === "ADDED" ? "Added" : "Removed"}{" "}
                    {formatNumber(record.shares, 4)} shares of {record.symbol}
                  </p>
                  <p className="subtle-text">{formatDate(record.date)}</p>
                  {record.action === "REMOVED" && (
                    <p className="subtle-text">
                      Remaining shares: {record.remainingShares ?? 0}
                    </p>
                  )}
                </li>
              ))}
            </ul>
          )}
        </article>
      </div>

      {performance?.status === "PARTIAL" && (
        <div className="banner banner-warning">
          Some prices are unavailable: {performance.missingPrices.join(", ")}.
        </div>
      )}

      <div className="summary-line">
        <p>Active symbols: {holdings.length}</p>
        <p>
          Total shares across active holdings: {formatNumber(totalShares, 4)}
        </p>
        <p>Portfolio base currency: {formatUsd(undefined)}</p>
      </div>
    </section>
  );
}

export default Dashboard;
