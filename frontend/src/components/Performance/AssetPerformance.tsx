import React from "react";
import {
  AggregatedHolding,
  PortfolioPerformance,
} from "../../types/portfolio";
import {
  formatNumber,
  formatPercent,
  formatSignedUsd,
  formatUsd,
} from "../../utils/formatters";

interface AssetPerformanceProps {
  holdings: AggregatedHolding[];
  performance: PortfolioPerformance | null;
}

/**
 * 中文：逐支股票展示数量、现价、平均成本、市值和未实现盈亏。
 * English: Displays quantity, current price, average cost, market value, and unrealized P/L per asset.
 */
function AssetPerformance({
  holdings,
  performance,
}: AssetPerformanceProps) {
  return (
    <article className="panel">
      <h3>Asset Performance</h3>
      {holdings.length === 0 ? (
        <p className="subtle-text">No active positions.</p>
      ) : (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Asset</th>
                <th scope="col" className="numeric-cell">
                  Shares
                </th>
                <th scope="col" className="numeric-cell">
                  Current Price
                </th>
                <th scope="col" className="numeric-cell">
                  Avg. Cost
                </th>
                <th scope="col" className="numeric-cell">
                  Market Value (USD)
                </th>
                <th scope="col" className="numeric-cell">
                  Unrealized P/L (USD)
                </th>
              </tr>
            </thead>
            <tbody>
              {holdings.map((holding) => {
                const asset = performance?.assets.find(
                  (candidate) => candidate.symbol === holding.symbol,
                );
                const valueClass =
                  asset?.unrealizedProfitLoss == null
                    ? ""
                    : asset.unrealizedProfitLoss >= 0
                      ? "value-positive"
                      : "value-negative";
                return (
                  <tr key={holding.symbol}>
                    <td>{holding.symbol}</td>
                    <td className="numeric-cell financial-value">
                      {formatNumber(holding.quantity, 4)}
                    </td>
                    <td className="numeric-cell financial-value">
                      {formatUsd(asset?.currentPrice)}
                    </td>
                    <td className="numeric-cell financial-value">
                      {formatUsd(asset?.averageCost)}
                    </td>
                    <td className="numeric-cell financial-value">
                      {formatUsd(asset?.currentValue)}
                    </td>
                    <td
                      className={`numeric-cell financial-value ${valueClass}`}
                    >
                      {formatSignedUsd(asset?.unrealizedProfitLoss)}
                      {asset?.returnPercentage != null && (
                        <span className="value-detail">
                          {formatPercent(asset.returnPercentage)}
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      {performance?.status === "PARTIAL" && (
        <p className="subtle-text">
          Missing current prices: {performance.missingPrices.join(", ")}.
        </p>
      )}
    </article>
  );
}

export default AssetPerformance;
