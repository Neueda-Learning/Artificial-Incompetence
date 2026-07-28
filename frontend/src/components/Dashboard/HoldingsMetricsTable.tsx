import React from "react";
import { AggregatedHolding } from "../../types/portfolio";
import { formatNumber } from "../../utils/formatters";

interface HoldingsMetricsTableProps {
  holdings: AggregatedHolding[];
}

function HoldingsMetricsTable({ holdings }: HoldingsMetricsTableProps) {
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
          {holdings.map((holding) => (
            <tr key={holding.symbol}>
              <td>
                <p>{holding.symbol}</p>
                <p className="subtle-text">{holding.assetType}</p>
              </td>
              <td className="numeric-cell financial-value">
                {formatNumber(holding.quantity, 4)}
              </td>
              <td className="numeric-cell financial-value">—</td>
              <td className="numeric-cell financial-value">—</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default HoldingsMetricsTable;
