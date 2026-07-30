import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Holdings from "./Holdings";
import {
  ActivityRecord,
  AggregatedHolding,
  PortfolioPerformance,
} from "../../types/portfolio";

const mockPerformance: PortfolioPerformance = {
  currency: "USD",
  totalCost: 5000,
  currentValue: 5500,
  unrealizedProfitLoss: 500,
  returnPercentage: 10,
  dayChange: 100,
  dayChangePercentage: 1.85,
  status: "COMPLETE",
  priceUpdatedAt: "2026-07-30T00:00:00Z",
  assets: [
    {
      symbol: "AAPL",
      quantity: 10,
      averageCost: 200,
      currentPrice: 220,
      costBasis: 2000,
      currentValue: 2200,
      unrealizedProfitLoss: 200,
      returnPercentage: 10,
      allocationPercentage: 45,
    },
    {
      symbol: "TSLA",
      quantity: 4,
      averageCost: 750,
      currentPrice: 825,
      costBasis: 3000,
      currentValue: 3300,
      unrealizedProfitLoss: 300,
      returnPercentage: 10,
      allocationPercentage: 55,
    },
  ],
  missingPrices: [],
};

const mockHoldings: AggregatedHolding[] = [
  { symbol: "AAPL", assetType: "STOCK", quantity: 10, sourceItemIds: [1] },
  { symbol: "TSLA", assetType: "STOCK", quantity: 4, sourceItemIds: [2] },
];

const mockActivities: ActivityRecord[] = [
  {
    id: "act-1",
    action: "ADDED",
    symbol: "AAPL",
    shares: 10,
    date: "2026-07-30T00:00:00Z",
    pricePerUnit: 200,
    currency: "USD",
  },
];

function renderHoldings(
  props: Partial<React.ComponentProps<typeof Holdings>> = {},
  initialRoute = "/holdings"
) {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      <Holdings
        holdings={[]}
        activities={[]}
        performance={null}
        isLoading={false}
        isPerformanceLoading={false}
        error={null}
        performanceError={null}
        onRetry={jest.fn()}
        {...props}
      />
    </MemoryRouter>
  );
}

describe("Holdings", () => {
  // ── Loading ────────────────────────────────────────────────────────────

  it("shows skeleton when loading", () => {
    renderHoldings({ isLoading: true });
    expect(document.querySelector(".skeleton-panel")).toBeInTheDocument();
  });

  // ── Error ──────────────────────────────────────────────────────────────

  it("shows error message with retry button", () => {
    const onRetry = jest.fn();
    renderHoldings({
      error: "Failed to load data",
      onRetry,
    });

    expect(screen.getByText("Unable to load holdings")).toBeInTheDocument();
    expect(screen.getByText("Failed to load data")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  // ── Tab navigation ─────────────────────────────────────────────────────

  it('renders three tabs with "positions" active by default', () => {
    renderHoldings({
      holdings: mockHoldings,
      performance: mockPerformance,
    });

    const tablist = screen.getByRole("tablist", { name: "Holdings sections" });

    const positionsTab = screen.getByRole("tab", { name: "Positions" });
    const allocationTab = screen.getByRole("tab", { name: "Allocation" });
    const historyTab = screen.getByRole("tab", { name: "History" });

    expect(positionsTab).toHaveAttribute("aria-selected", "true");
    expect(allocationTab).toHaveAttribute("aria-selected", "false");
    expect(historyTab).toHaveAttribute("aria-selected", "false");
  });

  it("shows active symbols count in header", () => {
    renderHoldings({
      holdings: mockHoldings,
      performance: mockPerformance,
    });

    expect(screen.getByText("2 active symbols")).toBeInTheDocument();
  });

  it("switches to allocation tab and shows allocation percentages", () => {
    renderHoldings({
      holdings: mockHoldings,
      performance: mockPerformance,
    });

    fireEvent.click(screen.getByRole("tab", { name: "Allocation" }));

    expect(screen.getByRole("tab", { name: "Allocation" })).toHaveAttribute(
      "aria-selected",
      "true"
    );

    // Allocation percentages should be visible
    expect(screen.getByText("45.00%")).toBeInTheDocument();
    expect(screen.getByText("55.00%")).toBeInTheDocument();
  });

  it("shows fallback when no allocation data", () => {
    const noAssetPerformance: PortfolioPerformance = {
      ...mockPerformance,
      assets: [],
    };

    renderHoldings({
      holdings: mockHoldings,
      performance: noAssetPerformance,
    });

    fireEvent.click(screen.getByRole("tab", { name: "Allocation" }));

    expect(
      screen.getByText("Allocation data is unavailable.")
    ).toBeInTheDocument();
  });

  it("switches to history tab and shows purchase history", () => {
    renderHoldings({
      holdings: mockHoldings,
      activities: mockActivities,
      performance: mockPerformance,
    });

    fireEvent.click(screen.getByRole("tab", { name: "History" }));

    expect(screen.getByRole("tab", { name: "History" })).toHaveAttribute(
      "aria-selected",
      "true"
    );
    expect(screen.getByText("AAPL")).toBeInTheDocument();
  });

  it("reads initial tab from URL search param", () => {
    renderHoldings(
      {
        holdings: mockHoldings,
        performance: mockPerformance,
      },
      "/holdings?tab=history"
    );

    expect(screen.getByRole("tab", { name: "History" })).toHaveAttribute(
      "aria-selected",
      "true"
    );
  });

  it("reads allocation tab from URL search param", () => {
    renderHoldings(
      {
        holdings: mockHoldings,
        performance: mockPerformance,
      },
      "/holdings?tab=allocation"
    );

    expect(screen.getByRole("tab", { name: "Allocation" })).toHaveAttribute(
      "aria-selected",
      "true"
    );
  });

  // ── Performance loading in positions tab ────────────────────────────────

  it("shows skeleton in positions tab when performance is loading", () => {
    renderHoldings({
      holdings: mockHoldings,
      performance: mockPerformance,
      isPerformanceLoading: true,
    });

    expect(document.querySelector(".skeleton-panel")).toBeInTheDocument();
  });

  // ── Performance error banner ────────────────────────────────────────────

  it("shows performance error banner", () => {
    renderHoldings({
      holdings: mockHoldings,
      performance: mockPerformance,
      performanceError: "Prices may be delayed",
    });

    expect(screen.getByText("Prices may be delayed")).toBeInTheDocument();
  });

  // ── Positions tab fallback ──────────────────────────────────────────────

  it("renders HoldingsPortfolio in positions tab with data", () => {
    renderHoldings({
      holdings: mockHoldings,
      performance: mockPerformance,
    });

    // Positions tab is default, so it should show the portfolio table
    expect(screen.getByText("Holdings")).toBeInTheDocument();
  });

  // ── Edge cases ──────────────────────────────────────────────────────────

  it('handles unknown tab param by defaulting to "positions"', () => {
    renderHoldings(
      {
        holdings: mockHoldings,
        performance: mockPerformance,
      },
      "/holdings?tab=unknown"
    );

    expect(screen.getByRole("tab", { name: "Positions" })).toHaveAttribute(
      "aria-selected",
      "true"
    );
  });

  it("shows allocation bar with zero-width for negative percentages", () => {
    const negativeAllocation: PortfolioPerformance = {
      ...mockPerformance,
      assets: [
        {
          symbol: "AAPL",
          quantity: 10,
          averageCost: 200,
          currentPrice: 220,
          costBasis: 2000,
          currentValue: 2200,
          unrealizedProfitLoss: 200,
          returnPercentage: 10,
          allocationPercentage: -5, // Negative
        },
      ],
    };

    renderHoldings({
      holdings: mockHoldings,
      performance: negativeAllocation,
    });

    fireEvent.click(screen.getByRole("tab", { name: "Allocation" }));

    // Allocation bar fill should clamp to 0%
    const fill = document.querySelector(".allocation-fill") as HTMLElement;
    expect(fill.style.width).toBe("0%");
  });
});
