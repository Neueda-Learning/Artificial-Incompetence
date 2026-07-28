import React from "react";
import { useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import {
  ActivityRecord,
  AggregatedHolding,
  PortfolioPerformance,
} from "../../types/portfolio";
import HoldingsPortfolio from "./HoldingsPortfolio";
import PurchaseHistory from "./PurchaseHistory";

type HoldingsTab = "positions" | "allocation" | "history";

interface HoldingsProps {
  holdings: AggregatedHolding[];
  activities: ActivityRecord[];
  performance: PortfolioPerformance | null;
  isLoading: boolean;
  isPerformanceLoading: boolean;
  error: string | null;
  performanceError: string | null;
  onRetry: () => void;
}

function Holdings({
  holdings,
  activities,
  performance,
  isLoading,
  isPerformanceLoading,
  error,
  performanceError,
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

      {performanceError && (
        <div className="banner banner-warning">{performanceError}</div>
      )}

      {currentTab === "positions" &&
        (isPerformanceLoading ? (
          <div className="skeleton-panel" />
        ) : (
          <HoldingsPortfolio
            holdings={holdings}
            performance={performance}
          />
        ))}

      {currentTab === "allocation" && (
        <article className="panel">
          <h3>Allocation</h3>
          {performance?.assets.length ? (
            <ul className="allocation-list">
              {performance.assets.map((asset) => (
                <li key={asset.symbol}>
                  <div className="allocation-row">
                    <span>{asset.symbol}</span>
                    <span>
                      {asset.allocationPercentage == null
                        ? "—"
                        : `${asset.allocationPercentage.toFixed(2)}%`}
                    </span>
                  </div>
                  <div className="allocation-track" aria-hidden="true">
                    <div
                      className="allocation-fill"
                      style={{
                        width: `${Math.max(0, asset.allocationPercentage ?? 0)}%`,
                      }}
                    />
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p className="subtle-text">Allocation data is unavailable.</p>
          )}
        </article>
      )}

      {currentTab === "history" && <PurchaseHistory activities={activities} />}
    </section>
  );
}

export default Holdings;
