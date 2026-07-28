import React from "react";
import { useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import {
  ActivityRecord,
  AggregatedHolding,
  AssetPerformance,
  Transaction,
} from "../../types/portfolio";
import HoldingsPortfolio from "./HoldingsPortfolio";
import PurchaseHistory from "./PurchaseHistory";
import { formatPercent } from "../../utils/formatters";

type HoldingsTab = "positions" | "allocation" | "history";

interface HoldingsProps {
  holdings: AggregatedHolding[];
  activities: ActivityRecord[];
  transactions: Transaction[];
  performanceBySymbol: Record<string, AssetPerformance>;
  isLoading: boolean;
  error: string | null;
  onRetry: () => void;
}

function Holdings({
  holdings,
  activities,
  transactions,
  performanceBySymbol,
  isLoading,
  error,
  onRetry,
}: HoldingsProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get("tab");
  const currentTab: HoldingsTab =
    tabParam === "allocation" || tabParam === "history"
      ? tabParam
      : "positions";

  const activeCountText = useMemo(
    () => `${holdings.length} active symbols`,
    [holdings.length],
  );

  if (isLoading) {
    return (
      <section className="page">
        <div className="skeleton-panel" />
      </section>
    );
  }

  if (error) {
    return (
      <section className="page">
        <div className="state-card">
          <h2>Unable to load holdings</h2>
          <p>{error}</p>
          <button
            type="button"
            className="button button-secondary"
            onClick={onRetry}
          >
            Retry
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="page">
      <div className="panel-header">
        <div>
          <h2>Holdings</h2>
          <p className="subtle-text">{activeCountText}</p>
        </div>
      </div>

      <div className="tab-strip" role="tablist" aria-label="Holdings sections">
        <button
          role="tab"
          aria-selected={currentTab === "positions"}
          className={currentTab === "positions" ? "tab active" : "tab"}
          onClick={() => setSearchParams({ tab: "positions" })}
        >
          Positions
        </button>
        <button
          role="tab"
          aria-selected={currentTab === "allocation"}
          className={currentTab === "allocation" ? "tab active" : "tab"}
          onClick={() => setSearchParams({ tab: "allocation" })}
        >
          Allocation
        </button>
        <button
          role="tab"
          aria-selected={currentTab === "history"}
          className={currentTab === "history" ? "tab active" : "tab"}
          onClick={() => setSearchParams({ tab: "history" })}
        >
          History
        </button>
      </div>

      {currentTab === "positions" && (
        <HoldingsPortfolio
          holdings={holdings}
          performanceBySymbol={performanceBySymbol}
        />
      )}

      {currentTab === "allocation" && (
        <article className="panel">
          <h3>Allocation</h3>
          {holdings.length === 0 ? (
            <p className="subtle-text">No active positions.</p>
          ) : (
            <ul className="allocation-list">
              {holdings.map((holding) => (
                <li key={holding.symbol} className="allocation-row">
                  <span>{holding.symbol}</span>
                  <span>
                    {formatPercent(
                      performanceBySymbol[holding.symbol]?.allocationPercentage,
                    )}
                  </span>
                </li>
              ))}
            </ul>
          )}
          <p className="subtle-text">
            Allocation values come from backend performance calculations.
          </p>
        </article>
      )}

      {currentTab === "history" && (
        <PurchaseHistory activities={activities} transactions={transactions} />
      )}
    </section>
  );
}

export default Holdings;
