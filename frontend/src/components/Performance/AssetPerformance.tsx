import React from "react";
import { AggregatedHolding } from "../../types/portfolio";
import { formatNumber } from "../../utils/formatters";

interface AssetPerformanceProps {
  holdings: AggregatedHolding[];
}

function AssetPerformance({ holdings }: AssetPerformanceProps) {
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
              {holdings.map((holding) => (
                <tr key={holding.symbol}>
                  <td>{holding.symbol}</td>
                  <td className="numeric-cell financial-value">
                    {formatNumber(holding.quantity, 4)}
                  </td>
                  <td className="numeric-cell financial-value">—</td>
                  <td className="numeric-cell financial-value">—</td>
                  <td className="numeric-cell financial-value">—</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="subtle-text">
        Native quote currencies, USD conversion, and P/L require backend market
        and valuation endpoints that are not yet exposed.
      </p>
    </article>
  );
}

export default AssetPerformance;
