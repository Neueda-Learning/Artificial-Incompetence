import React from "react";
import { useMemo, useState } from "react";
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

type SortKey = "symbol" | "shares";

interface HoldingsPortfolioProps {
  holdings: AggregatedHolding[];
  performance: PortfolioPerformance | null;
}

function HoldingsPortfolio({
  holdings,
  performance,
}: HoldingsPortfolioProps) {
  const [searchText, setSearchText] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("shares");
  const [isDesc, setIsDesc] = useState(true);

  const filteredRows = useMemo(() => {
    const normalizedSearch = searchText.trim().toUpperCase();
    const rows = holdings.filter((holding) =>
      normalizedSearch ? holding.symbol.includes(normalizedSearch) : true,
    );

    rows.sort((a, b) => {
      const comparison =
        sortKey === "shares"
          ? a.quantity - b.quantity
          : a.symbol.localeCompare(b.symbol);
      return isDesc ? -comparison : comparison;
    });

    return rows;
  }, [holdings, isDesc, searchText, sortKey]);

  const handleSort = (nextSortKey: SortKey) => {
    if (sortKey === nextSortKey) {
      setIsDesc((current) => !current);
      return;
    }
    setSortKey(nextSortKey);
    setIsDesc(nextSortKey === "shares");
  };

  if (holdings.length === 0) {
    return (
      <article className="panel">
        <h3>Positions</h3>
        <p className="subtle-text">No active positions.</p>
      </article>
    );
  }

  return (
    <article className="panel">
      <div className="panel-header">
        <h3>Positions</h3>
        <input
          type="search"
          placeholder="Search symbol"
          aria-label="Search symbol"
          value={searchText}
          onChange={(event) => setSearchText(event.target.value)}
        />
      </div>
      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">
                <button
                  type="button"
                  className="sort-button"
                  onClick={() => handleSort("symbol")}
                >
                  Asset {sortKey === "symbol" ? (isDesc ? "↓" : "↑") : ""}
                </button>
              </th>
              <th scope="col" className="numeric-cell">
                <button
                  type="button"
                  className="sort-button"
                  onClick={() => handleSort("shares")}
                >
                  Shares {sortKey === "shares" ? (isDesc ? "↓" : "↑") : ""}
                </button>
              </th>
              <th scope="col" className="numeric-cell">
                Current Price
              </th>
              <th scope="col" className="numeric-cell">
                Avg. Cost
              </th>
              <th scope="col" className="numeric-cell">
                Market Value
              </th>
              <th scope="col" className="numeric-cell">
                Unrealized P/L
              </th>
              <th scope="col" className="numeric-cell">
                Allocation
              </th>
            </tr>
          </thead>
          <tbody>
            {filteredRows.map((holding) => {
              const asset = performance?.assets.find(
                (candidate) => candidate.symbol === holding.symbol,
              );
              const profitLossClass =
                asset?.unrealizedProfitLoss == null
                  ? ""
                  : asset.unrealizedProfitLoss >= 0
                    ? "value-positive"
                    : "value-negative";

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
                    {formatUsd(asset?.currentPrice)}
                  </td>
                  <td className="numeric-cell financial-value">
                    {formatUsd(asset?.averageCost)}
                  </td>
                  <td className="numeric-cell financial-value">
                    {formatUsd(asset?.currentValue)}
                  </td>
                  <td
                    className={`numeric-cell financial-value ${profitLossClass}`}
                  >
                    {formatSignedUsd(asset?.unrealizedProfitLoss)}
                    {asset?.returnPercentage != null && (
                      <span className="value-detail">
                        {formatPercent(asset.returnPercentage)}
                      </span>
                    )}
                  </td>
                  <td className="numeric-cell financial-value">
                    {asset?.allocationPercentage == null
                      ? "—"
                      : `${asset.allocationPercentage.toFixed(2)}%`}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {performance?.status === "PARTIAL" && (
        <p className="subtle-text">
          Some prices are unavailable: {performance.missingPrices.join(", ")}.
        </p>
      )}
    </article>
  );
}

export default HoldingsPortfolio;
