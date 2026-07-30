import React from "react";
import { HistoricalPerformancePoint } from "../../types/portfolio";
import { formatDate, formatUsd } from "../../utils/formatters";

const CHART_WIDTH = 1000;
const CHART_HEIGHT = 260;
const CHART_LEFT = 34;
const CHART_RIGHT = 18;
const CHART_TOP = 18;
const CHART_BOTTOM = 24;

interface PerformanceLineChartProps {
  points: HistoricalPerformancePoint[];
  ariaLabel: string;
  isLoading?: boolean;
  error?: string | null;
  emptyMessage?: string;
  missingData?: string[];
}

interface LinePoint {
  source: HistoricalPerformancePoint;
  x: number;
  y: number;
}

function linePoints(points: HistoricalPerformancePoint[]): LinePoint[] {
  const values = points.map((point) => point.marketValue as number);
  const minimumValue = Math.min(...values);
  const maximumValue = Math.max(...values);
  const valueRange = maximumValue - minimumValue;
  const drawableWidth = CHART_WIDTH - CHART_LEFT - CHART_RIGHT;
  const drawableHeight = CHART_HEIGHT - CHART_TOP - CHART_BOTTOM;

  return points.map((point, index) => {
    const x =
      points.length === 1
        ? CHART_LEFT + drawableWidth / 2
        : CHART_LEFT + (index / (points.length - 1)) * drawableWidth;
    const y =
      valueRange === 0
        ? CHART_TOP + drawableHeight / 2
        : CHART_TOP +
          ((maximumValue - (point.marketValue as number)) / valueRange) *
            drawableHeight;
    return { source: point, x, y };
  });
}

function PerformanceLineChart({
  points,
  ariaLabel,
  isLoading = false,
  error = null,
  emptyMessage = "No historical valuation points are available for this range.",
  missingData = [],
}: PerformanceLineChartProps) {
  const chartPoints = points.filter((point) => point.marketValue != null);
  const plottedPoints = linePoints(chartPoints);
  const polyline = plottedPoints
    .map((point) => `${point.x},${point.y}`)
    .join(" ");

  if (isLoading) {
    return <div className="performance-chart-loading" aria-label="Loading chart" />;
  }

  if (error) {
    return (
      <div className="chart-placeholder">
        <p>{error}</p>
      </div>
    );
  }

  if (plottedPoints.length === 0) {
    return (
      <div className="chart-placeholder">
        <p>{emptyMessage}</p>
        {missingData.length > 0 && (
          <p className="subtle-text">Missing: {missingData.join(", ")}</p>
        )}
      </div>
    );
  }

  return (
    <div>
      <svg
        className="performance-line-chart"
        viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
        role="img"
        aria-label={ariaLabel}
        preserveAspectRatio="none"
      >
        <title>{ariaLabel}</title>
        {[0.25, 0.5, 0.75].map((position) => (
          <line
            key={position}
            className="chart-grid-line"
            x1={CHART_LEFT}
            x2={CHART_WIDTH - CHART_RIGHT}
            y1={
              CHART_TOP +
              position * (CHART_HEIGHT - CHART_TOP - CHART_BOTTOM)
            }
            y2={
              CHART_TOP +
              position * (CHART_HEIGHT - CHART_TOP - CHART_BOTTOM)
            }
          />
        ))}
        {plottedPoints.length > 1 && (
          <polyline
            className="performance-line"
            points={polyline}
            vectorEffect="non-scaling-stroke"
          />
        )}
        {plottedPoints.map((point) => (
          <circle
            key={point.source.date}
            className="performance-line-point"
            cx={point.x}
            cy={point.y}
            r={plottedPoints.length === 1 ? 6 : 4}
            vectorEffect="non-scaling-stroke"
          >
            <title>
              {`${formatDate(point.source.date)}: ${formatUsd(
                point.source.marketValue,
              )}`}
            </title>
          </circle>
        ))}
      </svg>
      <div className="chart-axis">
        <span>{formatDate(chartPoints[0].date)}</span>
        <span>{formatDate(chartPoints[chartPoints.length - 1].date)}</span>
      </div>
    </div>
  );
}

export default PerformanceLineChart;
