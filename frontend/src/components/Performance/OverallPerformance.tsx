import React, { useEffect, useState } from "react";
import {
  AggregatedHolding,
  HistoricalPerformance,
  PerformanceRange,
  PortfolioPerformance,
} from "../../types/portfolio";
import {
  formatPercent,
  formatSignedUsd,
  formatUsd,
} from "../../utils/formatters";
import PerformanceLineChart from "./PerformanceLineChart";

interface OverallPerformanceProps {
  holdings: AggregatedHolding[];
  performance: PortfolioPerformance | null;
  history: HistoricalPerformance | null;
  selectedRange: PerformanceRange;
  isHistoryLoading: boolean;
  historyError: string | null;
}

const OVERALL_SERIES_ID = "OVERALL";

/**
 * 中文：在总体组合和单支股票之间切换，展示对应指标与历史折线。
 * English: Switches between the whole portfolio and individual stocks to show matching metrics and history.
 */
function OverallPerformance({
  holdings,
  performance,
  history,
  selectedRange,
  isHistoryLoading,
  historyError,
}: OverallPerformanceProps) {
  const [selectedSeriesId, setSelectedSeriesId] =
    useState(OVERALL_SERIES_ID);

  // 中文：当已选股票不再存在于最新响应中时，自动退回总体视图。
  // English: Falls back to the overall view when the selected asset is absent from the latest response.
  useEffect(() => {
    if (
      selectedSeriesId !== OVERALL_SERIES_ID &&
      !history?.assets.some((asset) => asset.id === selectedSeriesId)
    ) {
      setSelectedSeriesId(OVERALL_SERIES_ID);
    }
  }, [history, selectedSeriesId]);

  const selectedSeries = history?.assets.find(
    (asset) => asset.id === selectedSeriesId,
  );
  const isOverall = selectedSeriesId === OVERALL_SERIES_ID;
  const selectedCurrentPerformance = selectedSeries
    ? performance?.assets.find(
        (asset) => asset.symbol === selectedSeries.symbol,
      )
    : null;

  const chartPoints =
    (isOverall ? history?.points : selectedSeries?.points)?.filter(
      (point) => point.marketValue != null,
    ) ?? [];

  const heading = isOverall
    ? "Portfolio Value"
    : `${selectedSeries?.symbol ?? "Asset"} Performance`;
  const currentValue = isOverall
    ? performance?.currentValue
    : selectedCurrentPerformance?.currentValue;
  const returnPercentage = isOverall
    ? performance?.returnPercentage
    : selectedCurrentPerformance?.returnPercentage;
  const profitLoss = isOverall
    ? performance?.unrealizedProfitLoss
    : selectedCurrentPerformance?.unrealizedProfitLoss;
  const costBasis = isOverall
    ? performance?.totalCost
    : selectedCurrentPerformance?.costBasis;
  const missingForSelection = isOverall
    ? history?.missingData ?? []
    : (history?.missingData ?? []).filter((item) =>
        item.startsWith(`${selectedSeries?.symbol}:`),
      );

  return (
    <article className="panel panel-large">
      <div className="performance-series-header">
        <div>
          <h3>{heading}</h3>
          <p className="subtle-text">Selected range: {selectedRange}</p>
        </div>
        <div
          className="series-switch"
          role="group"
          aria-label="Performance series"
        >
          <button
            type="button"
            className={isOverall ? "series-button active" : "series-button"}
            onClick={() => setSelectedSeriesId(OVERALL_SERIES_ID)}
          >
            Overall
          </button>
          {history?.assets
            .filter((asset) => asset.assetType === "STOCK")
            .map((asset) => (
              <button
                key={asset.id}
                type="button"
                className={
                  selectedSeriesId === asset.id
                    ? "series-button active"
                    : "series-button"
                }
                onClick={() => setSelectedSeriesId(asset.id)}
                title={`${asset.assetType} · ${asset.currency}`}
              >
                {asset.symbol}
              </button>
            ))}
        </div>
      </div>

      <div className="metrics-grid metrics-grid-compact">
        <div className="metric-card">
          <h4>{isOverall ? "Total Portfolio Value" : "Current Value"}</h4>
          <p className="metric-value">{formatUsd(currentValue)}</p>
        </div>
        <div className="metric-card">
          <h4>{isOverall ? "Total Return" : "Asset Return"}</h4>
          <p
            className={`metric-value ${
              (returnPercentage ?? 0) >= 0
                ? "value-positive"
                : "value-negative"
            }`}
          >
            {formatPercent(returnPercentage)}
          </p>
        </div>
        <div className="metric-card">
          <h4>Unrealized P/L</h4>
          <p
            className={`metric-value ${
              (profitLoss ?? 0) >= 0 ? "value-positive" : "value-negative"
            }`}
          >
            {formatSignedUsd(profitLoss)}
          </p>
        </div>
        <div className="metric-card">
          <h4>{isOverall ? "Total Cost Basis" : "Asset Cost Basis"}</h4>
          <p className="metric-value">{formatUsd(costBasis)}</p>
        </div>
      </div>

      <PerformanceLineChart
        points={chartPoints}
        ariaLabel={`${heading} history for ${selectedRange}`}
        isLoading={isHistoryLoading}
        error={historyError}
        missingData={missingForSelection}
      />

      {history?.status === "PARTIAL" && missingForSelection.length > 0 && (
        <div className="banner banner-warning">
          Some historical data is missing: {missingForSelection.join(", ")}.
        </div>
      )}
      {holdings.length === 0 && (
        <p className="subtle-text">No active holdings.</p>
      )}
    </article>
  );
}

export default OverallPerformance;
