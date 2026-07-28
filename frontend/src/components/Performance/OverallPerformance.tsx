import React from "react";
import { AggregatedHolding } from "../../types/portfolio";

interface OverallPerformanceProps {
  holdings: AggregatedHolding[];
  selectedRange: string;
}

function OverallPerformance({
  holdings,
  selectedRange,
}: OverallPerformanceProps) {
  return (
    <article className="panel panel-large">
      <h3>Portfolio Value</h3>
      <p className="subtle-text">Selected range: {selectedRange}</p>
      <div className="metrics-grid metrics-grid-compact">
        <div className="metric-card">
          <h4>Total Portfolio Value</h4>
          <p className="metric-value">—</p>
        </div>
        <div className="metric-card">
          <h4>Total Return</h4>
          <p className="metric-value">—</p>
        </div>
        <div className="metric-card">
          <h4>Unrealized P/L</h4>
          <p className="metric-value">—</p>
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
          Historical USD valuation is unavailable from the current backend
          response fields.
        </p>
        <p className="subtle-text">
          Do not infer or synthesize financial history in frontend.
        </p>
      </div>
      {holdings.length === 0 && (
        <p className="subtle-text">No active holdings.</p>
      )}
    </article>
  );
}

export default OverallPerformance;
