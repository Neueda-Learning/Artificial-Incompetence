import React, { useEffect, useState } from "react";
import { getHistoricalPerformance } from "../../services/portfolioService";
import {
  AggregatedHolding,
  HistoricalPerformance,
  PerformanceRange,
  PortfolioPerformance,
} from "../../types/portfolio";
import AssetPerformance from "./AssetPerformance";
import OverallPerformance from "./OverallPerformance";

interface PerformanceProps {
  holdings: AggregatedHolding[];
  portfolioPerformance: PortfolioPerformance | null;
  performanceBySymbol: Record<string, AssetPerformanceRow>;
  isLoading: boolean;
  isPerformanceLoading: boolean;
  error: string | null;
  performanceError: string | null;
  onRetry: () => void;
}

const RANGES: PerformanceRange[] = ["1W", "1M", "3M", "1Y", "ALL"];

function Performance({
  holdings,
  portfolioPerformance,
  performanceBySymbol,
  isLoading,
  isPerformanceLoading,
  error,
  performanceError,
  onRetry,
}: PerformanceProps) {
  const [selectedRange, setSelectedRange] =
    useState<PerformanceRange>("1M");
  const [history, setHistory] = useState<HistoricalPerformance | null>(null);
  const [isHistoryLoading, setIsHistoryLoading] = useState(true);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [historyRequestId, setHistoryRequestId] = useState(0);

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
          setHistoryError("Unable to load historical performance.");
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
  }, [historyRequestId, selectedRange]);

  if (isLoading || isPerformanceLoading) {
    return (
      <section className="page">
        <div className="skeleton-panel" />
      </section>
    );
  }

  if (error || (!performance && performanceError)) {
    return (
      <section className="page">
        <div className="state-card">
          <h2>Unable to load performance</h2>
          <p>{error ?? performanceError}</p>
          <button
            type="button"
            className="button button-secondary"
            onClick={() => {
              onRetry();
              setHistoryRequestId((current) => current + 1);
            }}
          >
            Retry
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="page">
      <div className="panel-header">
        <h2>Performance</h2>
        <div
          className="range-group"
          role="group"
          aria-label="Performance ranges"
        >
          {RANGES.map((range) => (
            <button
              key={range}
              type="button"
              className={
                selectedRange === range ? "range-button active" : "range-button"
              }
              onClick={() => setSelectedRange(range)}
            >
              {range}
            </button>
          ))}
        </div>
      </div>

      {performanceError && (
        <div className="banner banner-warning">{performanceError}</div>
      )}

      <OverallPerformance
        holdings={holdings}
        performance={performance}
        history={history}
        selectedRange={selectedRange}
        isHistoryLoading={isHistoryLoading}
        historyError={historyError}
      />
      <AssetPerformance holdings={holdings} performance={performance} />
    </section>
  );
}

export default Performance;
