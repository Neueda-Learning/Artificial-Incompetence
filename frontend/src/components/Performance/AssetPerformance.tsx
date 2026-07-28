import React from "react";
import { AggregatedHolding, AssetPerformance as AssetPerformanceRow } from "../../types/portfolio";
import {
  formatNumber,
  formatSignedUsd,
  formatUsd,
} from "../../utils/formatters";

interface AssetPerformanceProps {
  holdings: AggregatedHolding[];
  performanceBySymbol: Record<string, AssetPerformanceRow>;
}

function AssetPerformance({
  holdings,
  performanceBySymbol,
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
                  Market Value (USD)
                </th>
                <th scope="col" className="numeric-cell">
                  Unrealized P/L (USD)
                </th>
              </tr>
            </thead>
            <tbody>
              {holdings.map((holding) => {
                const performance = performanceBySymbol[holding.symbol];
                return (
                <tr key={holding.symbol}>
                  <td>{holding.symbol}</td>
                  <td className="numeric-cell financial-value">
                    {formatNumber(holding.quantity, 4)}
                  </td>
                  <td className="numeric-cell financial-value">
                    {formatUsd(performance?.currentPrice)}
                  </td>
                  <td className="numeric-cell financial-value">
                    {formatUsd(performance?.currentValue)}
                  </td>
                  <td className="numeric-cell financial-value">
                    {formatSignedUsd(performance?.unrealizedProfitLoss)}
                  </td>
                </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      <p className="subtle-text">
        Metrics above come from backend performance endpoint and are normalized
        to USD where available.
      </p>
    </article>
  );
}

export default AssetPerformance;
