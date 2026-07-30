import React, { useEffect, useState } from "react";
import { getHistoricalPerformance } from "../../services/portfolioService";
import {
  AggregatedHolding,
  HistoricalPerformance,
  PerformanceRange,
  PortfolioPerformance,
} from "../../types/portfolio";
import { formatNumber, formatUsd } from "../../utils/formatters";
import PerformanceLineChart from "../Performance/PerformanceLineChart";

interface MarketIndicatorsProps {
  holdings: AggregatedHolding[];
  performance: PortfolioPerformance | null;
}

const DASHBOARD_RANGES: PerformanceRange[] = [
  "1W",
  "1M",
  "3M",
  "1Y",
  "ALL",
];

function MarketIndicators({
  holdings,
  performance,
}: MarketIndicatorsProps) {
  const [selectedRange, setSelectedRange] =
    useState<PerformanceRange>("1M");
  const [history, setHistory] = useState<HistoricalPerformance | null>(null);
  const [isHistoryLoading, setIsHistoryLoading] = useState(true);
  const [historyError, setHistoryError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;
    setIsHistoryLoading(true);
    setHistoryError(null);

    getHistoricalPerformance(selectedRange)
      .then((response) => {
        if (isActive) {
          setHistory(response);
        }
      })
      .catch(() => {
        if (isActive) {
          setHistoryError("Unable to load portfolio history.");
        }
      })
      .finally(() => {
        if (isActive) {
          setIsHistoryLoading(false);
        }
      });

    return () => {
      isActive = false;
    };
  }, [selectedRange]);

  return (
    <section className="two-column-layout">
      <article className="panel panel-large">
        <div className="panel-header dashboard-performance-header">
          <div>
            <h2>Portfolio Performance</h2>
            <p className="subtle-text">
              Overall value · {formatUsd(performance?.currentValue)}
            </p>
          </div>
          <div
            className="range-group"
            role="group"
            aria-label="Dashboard performance ranges"
          >
            {DASHBOARD_RANGES.map((range) => (
              <button
                key={range}
                type="button"
                className={
                  selectedRange === range
                    ? "range-button active"
                    : "range-button"
                }
                onClick={() => setSelectedRange(range)}
              >
                {range}
              </button>
            ))}
          </div>
        </div>
        <PerformanceLineChart
          points={history?.points ?? []}
          ariaLabel={`Overall portfolio performance history for ${selectedRange}`}
          isLoading={isHistoryLoading}
          error={historyError}
          missingData={history?.missingData ?? []}
        />
        {history?.status === "PARTIAL" &&
          history.missingData.length > 0 && (
            <p className="subtle-text dashboard-history-warning">
              Some historical valuations are incomplete.
            </p>
          )}
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
