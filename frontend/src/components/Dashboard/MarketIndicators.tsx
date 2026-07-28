import React from "react";
import {
  AggregatedHolding,
  PortfolioPerformance,
} from "../../types/portfolio";
import { formatNumber, formatUsd } from "../../utils/formatters";

interface MarketIndicatorsProps {
  holdings: AggregatedHolding[];
  performance: PortfolioPerformance | null;
}

function MarketIndicators({
  holdings,
  performance,
}: MarketIndicatorsProps) {
  return (
    <section className="two-column-layout">
      <article className="panel panel-large">
        <h2>Portfolio Performance</h2>
        <div className="dashboard-performance-summary">
          <p className="metric-value">
            {formatUsd(performance?.currentValue)}
          </p>
          <p className="subtle-text">
            Current portfolio value · Open Performance for the historical
            series.
          </p>
        </div>
      </article>
      <article className="panel">
        <h2>Asset Allocation</h2>
        {holdings.length === 0 ? (
          <p className="subtle-text">No active positions.</p>
        ) : (
          <ul className="allocation-list">
            {holdings.slice(0, 6).map((holding) => {
              const asset = performance?.assets.find(
                (candidate) => candidate.symbol === holding.symbol,
              );
              const ratio = asset?.allocationPercentage ?? 0;
              return (
                <li key={holding.symbol}>
                  <div className="allocation-row">
                    <span>{holding.symbol}</span>
                    <span>{ratio.toFixed(2)}%</span>
                  </div>
                  <div className="allocation-track" aria-hidden="true">
                    <div
                      className="allocation-fill"
                      style={{ width: `${Math.max(6, ratio)}%` }}
                    />
                  </div>
                  <p className="subtle-text">
                    {formatNumber(holding.quantity, 4)} shares ·{" "}
                    {formatUsd(asset?.currentValue)}
                  </p>
                </li>
              );
            })}
          </ul>
        )}
        {performance?.status === "PARTIAL" && (
          <p className="subtle-text">
            Allocation excludes assets without a current price.
          </p>
        )}
      </article>
    </section>
  );
}

export default MarketIndicators;
