import React from "react";
import {
  AggregatedHolding,
  HistoricalPerformance,
  PerformanceRange,
  PortfolioPerformance,
} from "../../types/portfolio";
import {
  formatDate,
  formatPercent,
  formatSignedUsd,
  formatUsd,
} from "../../utils/formatters";

interface OverallPerformanceProps {
  holdings: AggregatedHolding[];
  performance: PortfolioPerformance | null;
  history: HistoricalPerformance | null;
  selectedRange: PerformanceRange;
  isHistoryLoading: boolean;
  historyError: string | null;
}

function OverallPerformance({
  holdings,
  performance,
  history,
  selectedRange,
  isHistoryLoading,
  historyError,
}: OverallPerformanceProps) {
  const chartPoints =
    history?.points.filter((point) => point.marketValue != null) ?? [];
  const values = chartPoints.map((point) => point.marketValue as number);
  const minimumValue = values.length ? Math.min(...values) : 0;
  const maximumValue = values.length ? Math.max(...values) : 0;
  const valueRange = maximumValue - minimumValue;

  return (
    <article className="panel panel-large">
      <h3>Portfolio Value</h3>
      <p className="subtle-text">Selected range: {selectedRange}</p>
      <div className="metrics-grid metrics-grid-compact">
        <div className="metric-card">
          <h4>Total Portfolio Value</h4>
          <p className="metric-value">
            {formatUsd(performance?.currentValue)}
          </p>
        </div>
        <div className="metric-card">
          <h4>Total Return</h4>
          <p
            className={`metric-value ${
              (performance?.returnPercentage ?? 0) >= 0
                ? "value-positive"
                : "value-negative"
            }`}
          >
            {formatPercent(performance?.returnPercentage)}
          </p>
        </div>
        <div className="metric-card">
          <h4>Unrealized P/L</h4>
          <p
            className={`metric-value ${
              (performance?.unrealizedProfitLoss ?? 0) >= 0
                ? "value-positive"
                : "value-negative"
            }`}
          >
            {formatSignedUsd(performance?.unrealizedProfitLoss)}
          </p>
        </div>
        <div className="metric-card">
          <h4>Total Cost Basis</h4>
          <p className="metric-value">{formatUsd(performance?.totalCost)}</p>
        </div>
      </div>
      {isHistoryLoading ? (
        <div className="performance-chart-loading" aria-label="Loading chart" />
      ) : historyError ? (
        <div className="chart-placeholder">
          <p>{historyError}</p>
        </div>
      ) : chartPoints.length ? (
        <div>
          <div
            className="performance-chart"
            role="img"
            aria-label={`Portfolio value history for ${selectedRange}`}
          >
            {chartPoints.map((point) => {
              const normalizedHeight =
                valueRange === 0
                  ? 55
                  : 18 +
                    (((point.marketValue as number) - minimumValue) /
                      valueRange) *
                      72;
              return (
                <div
                  key={point.date}
                  className="performance-bar"
                  style={{ height: `${normalizedHeight}%` }}
                  title={`${formatDate(point.date)}: ${formatUsd(point.marketValue)}`}
                />
              );
            })}
          </div>
          <div className="chart-axis">
            <span>{formatDate(chartPoints[0].date)}</span>
            <span>{formatDate(chartPoints[chartPoints.length - 1].date)}</span>
          </div>
        </div>
      ) : (
        <div className="chart-placeholder">
          <p>No historical valuation points are available for this range.</p>
          {history?.missingData.length ? (
            <p className="subtle-text">
              Missing: {history.missingData.join(", ")}
            </p>
          ) : null}
        </div>
      )}
      {history?.status === "PARTIAL" && history.missingData.length > 0 && (
        <div className="banner banner-warning">
          Some historical data is missing: {history.missingData.join(", ")}.
        </div>
      )}
      {holdings.length === 0 && (
        <p className="subtle-text">No active holdings.</p>
      )}
    </article>
  );
}

export default OverallPerformance;
