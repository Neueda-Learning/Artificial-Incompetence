import React from "react";
import { useState } from "react";
import { AggregatedHolding } from "../../types/portfolio";
import AssetPerformance from "./AssetPerformance";
import OverallPerformance from "./OverallPerformance";

interface PerformanceProps {
  holdings: AggregatedHolding[];
  isLoading: boolean;
  error: string | null;
  onRetry: () => void;
}

const RANGES = ["1M", "3M", "6M", "YTD", "1Y", "ALL"];

function Performance({
  holdings,
  isLoading,
  error,
  onRetry,
}: PerformanceProps) {
  const [selectedRange, setSelectedRange] = useState("1M");

  if (isLoading) {
    return (
      <section className="page">
        <div className="skeleton-panel" />
      </section>
    );
  }

  if (error) {
    return (
      <section className="page">
        <div className="state-card">
          <h2>Unable to load performance</h2>
          <p>{error}</p>
          <button
            type="button"
            className="button button-secondary"
            onClick={onRetry}
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
              disabled
            >
              {range}
            </button>
          ))}
        </div>
      </div>

      <OverallPerformance holdings={holdings} selectedRange={selectedRange} />
      <AssetPerformance holdings={holdings} />
    </section>
  );
}

export default Performance;
