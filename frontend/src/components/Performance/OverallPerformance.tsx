import React from "react";
import { AggregatedHolding, PortfolioPerformance } from "../../types/portfolio";
import {
  formatPercent,
  formatSignedUsd,
  formatUsd,
} from "../../utils/formatters";

interface OverallPerformanceProps {
  holdings: AggregatedHolding[];
  selectedRange: string;
  portfolioPerformance: PortfolioPerformance | null;
}

function OverallPerformance({
  holdings,
  selectedRange,
  portfolioPerformance,
}: OverallPerformanceProps) {
  return (
    <article className="panel panel-large">
      <h3>Portfolio Value</h3>
      <p className="subtle-text">Selected range: {selectedRange}</p>
      <div className="metrics-grid metrics-grid-compact">
        <div className="metric-card">
          <h4>Total Portfolio Value</h4>
          <p className="metric-value">{formatUsd(portfolioPerformance?.currentValue)}</p>
        </div>
        <div className="metric-card">
          <h4>Total Return</h4>
          <p className="metric-value">
            {formatPercent(portfolioPerformance?.returnPercentage)}
          </p>
        </div>
        <div className="metric-card">
          <h4>Unrealized P/L</h4>
          <p className="metric-value">
            {formatSignedUsd(portfolioPerformance?.unrealizedProfitLoss)}
          </p>
        </div>
        <div className="metric-card">
          <h4>Day Change</h4>
          <p className="metric-value">—</p>
        </div>
      </div>
      <div
        className="chart-placeholder"
        role="img"
        aria-label="Performance chart unavailable"
      >
        <p>
          Historical USD valuation timeseries is unavailable from backend API.
        </p>
        <p className="subtle-text">
          Current totals are live, but range buttons remain disabled until
          historical snapshots are exposed.
        </p>
      </div>
      {portfolioPerformance?.status === "PARTIAL" && (
        <p className="subtle-text">
          Missing prices: {portfolioPerformance.missingPrices.join(", ")}.
        </p>
      )}
      {holdings.length === 0 && (
        <p className="subtle-text">No active holdings.</p>
      )}
    </article>
  );
}

export default OverallPerformance;
