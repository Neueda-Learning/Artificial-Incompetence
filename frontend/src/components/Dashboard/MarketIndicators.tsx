import React from "react";
import { AggregatedHolding } from "../../types/portfolio";
import { formatNumber } from "../../utils/formatters";

interface MarketIndicatorsProps {
  holdings: AggregatedHolding[];
}

function MarketIndicators({ holdings }: MarketIndicatorsProps) {
  const totalShares = holdings.reduce(
    (total, holding) => total + holding.quantity,
    0,
  );

  return (
    <section className="two-column-layout">
      <article className="panel panel-large">
        <h2>Portfolio Performance</h2>
        <div
          className="chart-placeholder"
          role="img"
          aria-label="Performance chart unavailable"
        >
          <p>USD performance time series is unavailable.</p>
          <p className="subtle-text">
            The current backend contract does not provide historical portfolio
            values.
          </p>
        </div>
      </article>
      <article className="panel">
        <h2>Asset Allocation</h2>
        {holdings.length === 0 ? (
          <p className="subtle-text">No active positions.</p>
        ) : (
          <ul className="allocation-list">
            {holdings.slice(0, 6).map((holding) => {
              const ratio =
                totalShares > 0 ? (holding.quantity / totalShares) * 100 : 0;
              return (
                <li key={holding.symbol}>
                  <div className="allocation-row">
                    <span>{holding.symbol}</span>
                    <span>{ratio.toFixed(2)}%</span>
                  </div>
                  <div className="allocation-track" aria-hidden="true">
                    <div
                      className="allocation-fill"
                      style={{ width: `${Math.max(6, ratio)}%` }}
                    />
                  </div>
                  <p className="subtle-text">
                    {formatNumber(holding.quantity, 4)} shares · USD market
                    value unavailable
                  </p>
                </li>
              );
            })}
          </ul>
        )}
        <p className="subtle-text">
          Allocation percentages above are share-based placeholders until
          backend USD market values are available.
        </p>
      </article>
    </section>
  );
}

export default MarketIndicators;
