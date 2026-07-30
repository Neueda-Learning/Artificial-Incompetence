import React from "react";
import {
  AggregatedHolding,
  PortfolioPerformance,
} from "../../types/portfolio";
import {
  formatNumber,
  formatSignedUsd,
  formatUsd,
} from "../../utils/formatters";

interface HoldingsMetricsTableProps {
  holdings: AggregatedHolding[];
  performance: PortfolioPerformance | null;
}

function HoldingsMetricsTable({
  holdings,
  performance,
}: HoldingsMetricsTableProps) {
  if (holdings.length === 0) {
    return <p className="subtle-text">No active holdings.</p>;
  }

  return (
    <div className="table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Asset</th>
            <th scope="col" className="numeric-cell">
              Shares
            </th>
            <th scope="col" className="numeric-cell">
              Market Value
            </th>
            <th scope="col" className="numeric-cell">
              Unrealized P/L
            </th>
          </tr>
        </thead>
        <tbody>
          {holdings.map((holding) => {
            const asset = performance?.assets.find(
              (candidate) => candidate.symbol === holding.symbol,
            );
            return (
              <tr key={holding.symbol}>
                <td>
                  <p>{holding.symbol}</p>
                  <p className="subtle-text">{holding.assetType}</p>
                </td>
                <td className="numeric-cell financial-value">
                  {formatNumber(holding.quantity, 4)}
                </td>
                <td className="numeric-cell financial-value">
                  {formatUsd(asset?.currentValue)}
                </td>
                <td
                  className={`numeric-cell financial-value ${
                    asset?.unrealizedProfitLoss == null
                      ? ""
                      : asset.unrealizedProfitLoss >= 0
                        ? "value-positive"
                        : "value-negative"
                  }`}
                >
                  {formatSignedUsd(asset?.unrealizedProfitLoss)}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

export default HoldingsMetricsTable;
