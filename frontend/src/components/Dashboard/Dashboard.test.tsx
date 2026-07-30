import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Dashboard from "./Dashboard";
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
      allocationPercentage: 40,
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
      allocationPercentage: 60,
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
  {
    id: "act-2",
    action: "ADDED",
    symbol: "TSLA",
    shares: 4,
    date: "2026-07-29T00:00:00Z",
    pricePerUnit: 750,
    currency: "USD",
  },
];

const removalActivity: ActivityRecord = {
  id: "act-3",
  action: "REMOVED",
  symbol: "AAPL",
  shares: 2,
  date: "2026-07-28T00:00:00Z",
  remainingShares: 8,
};

function renderDashboard(props: Partial<React.ComponentProps<typeof Dashboard>> = {}) {
  return render(
    <MemoryRouter>
      <Dashboard
        holdings={[]}
        activities={[]}
        performance={null}
        isLoading={false}
        isPerformanceLoading={false}
        error={null}
        performanceError={null}
        onRetry={jest.fn()}
        onAddAsset={jest.fn()}
        onRemoveAsset={jest.fn()}
        {...props}
      />
    </MemoryRouter>
  );
}

describe("Dashboard", () => {
  // ── Loading ────────────────────────────────────────────────────────────

  it("shows skeleton when data is loading", () => {
    renderDashboard({ isLoading: true });
    expect(document.querySelector(".skeleton-grid")).toBeInTheDocument();
    expect(document.querySelector(".skeleton-panel")).toBeInTheDocument();
  });

  it("shows skeleton when performance is loading", () => {
    renderDashboard({ isPerformanceLoading: true });
    expect(document.querySelector(".skeleton-grid")).toBeInTheDocument();
  });

  // ── Error ──────────────────────────────────────────────────────────────

  it("shows error message with retry button", () => {
    const onRetry = jest.fn();
    renderDashboard({
      error: "Network error",
      onRetry,
    });

    expect(screen.getByText("Unable to load dashboard")).toBeInTheDocument();
    expect(screen.getByText("Network error")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  // ── Empty state ─────────────────────────────────────────────────────────

  it("shows onboarding message when portfolio is empty", () => {
    const onAddAsset = jest.fn();
    renderDashboard({
      holdings: [],
      activities: [],
      onAddAsset,
    });

    expect(
      screen.getByText("Start building your portfolio")
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Add your first asset to track portfolio value/)
    ).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: "Add your first asset" })
    );
    expect(onAddAsset).toHaveBeenCalledTimes(1);
  });

  it("shows no-current-holdings state when activities exist but holdings are empty", () => {
    renderDashboard({
      holdings: [],
      activities: [removalActivity],
    });

    expect(screen.getByText("No current holdings")).toBeInTheDocument();
    expect(
      screen.getByText(/All assets have been removed/)
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Add Asset" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "View History" })).toHaveAttribute(
      "href",
      "/holdings?tab=history"
    );
  });

  // ── Normal rendering ────────────────────────────────────────────────────

  it("renders four metric cards with formatted values", () => {
    renderDashboard({
      holdings: mockHoldings,
      activities: mockActivities,
      performance: mockPerformance,
    });

    // Total Portfolio Value
    expect(screen.getByText("Total Portfolio Value")).toBeInTheDocument();
    expect(screen.getByText("USD 5,500.00")).toBeInTheDocument();

    // Total Return
    expect(screen.getByText("Total Return")).toBeInTheDocument();
    expect(screen.getByText("+10.00%")).toBeInTheDocument();

    // Day Change
    expect(screen.getByText("Day Change")).toBeInTheDocument();
    expect(screen.getByText("+USD 100.00")).toBeInTheDocument();
    expect(
      screen.getByText("+1.85% since the previous market close.")
    ).toBeInTheDocument();

    // Total Cost Basis
    expect(screen.getByText("Total Cost Basis")).toBeInTheDocument();
    expect(screen.getByText("USD 5,000.00")).toBeInTheDocument();
    expect(screen.getByText(/Unrealized P\/L:/)).toBeInTheDocument();
    expect(
      screen.getByText(/Unrealized P\/L:\s*\+USD 500\.00/),
    ).toBeInTheDocument();
  });

  it("renders negative values with negative styling", () => {
    const negativePerformance: PortfolioPerformance = {
      ...mockPerformance,
      totalCost: 6000,
      currentValue: 5500,
      unrealizedProfitLoss: -500,
      returnPercentage: -8.33,
      dayChange: -100,
      dayChangePercentage: -1.5,
    };

    renderDashboard({
      holdings: mockHoldings,
      activities: mockActivities,
      performance: negativePerformance,
    });

    expect(screen.getByText("−8.33%")).toBeInTheDocument();
    expect(screen.getByText("−USD 100.00")).toBeInTheDocument();
    expect(
      screen.getByText(/Unrealized P\/L:\s*−USD 500\.00/),
    ).toBeInTheDocument();
  });

  it('renders "Top Holdings" link and table', () => {
    renderDashboard({
      holdings: mockHoldings,
      activities: mockActivities,
      performance: mockPerformance,
    });

    expect(screen.getByText("Top Holdings")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "View all holdings" })
    ).toHaveAttribute("href", "/holdings");
  });

  it("renders recent activity list", () => {
    renderDashboard({
      holdings: mockHoldings,
      activities: mockActivities,
      performance: mockPerformance,
    });

    expect(screen.getByText("Recent Activity")).toBeInTheDocument();
    expect(screen.getByText(/10 shares of AAPL/)).toBeInTheDocument();
    expect(screen.getByText(/4 shares of TSLA/)).toBeInTheDocument();
  });

  it("shows message when no activity recorded", () => {
    renderDashboard({
      holdings: mockHoldings,
      activities: [],
      performance: mockPerformance,
    });

    expect(screen.getByText("No activity recorded yet.")).toBeInTheDocument();
  });

  it("shows removal activity with remaining shares", () => {
    renderDashboard({
      holdings: mockHoldings,
      activities: [removalActivity],
      performance: mockPerformance,
    });

    expect(screen.getByText(/2 shares of AAPL/)).toBeInTheDocument();
    expect(screen.getByText("Remaining shares: 8")).toBeInTheDocument();
  });

  it('calls onRemoveAsset when "Remove Asset" button is clicked', () => {
    const onRemoveAsset = jest.fn();
    renderDashboard({
      holdings: mockHoldings,
      activities: mockActivities,
      performance: mockPerformance,
      onRemoveAsset,
    });

    fireEvent.click(screen.getByRole("button", { name: "Remove Asset" }));
    expect(onRemoveAsset).toHaveBeenCalledTimes(1);
  });

  // ── Performance warnings ────────────────────────────────────────────────

  it("shows performance error banner when present", () => {
    renderDashboard({
      holdings: mockHoldings,
      activities: mockActivities,
      performance: mockPerformance,
      performanceError: "Market data temporarily unavailable",
    });

    expect(
      screen.getByText("Market data temporarily unavailable")
    ).toBeInTheDocument();
  });

  it("shows partial performance warning with missing symbols", () => {
    const partialPerformance: PortfolioPerformance = {
      ...mockPerformance,
      status: "PARTIAL",
      missingPrices: ["TSLA"],
    };

    renderDashboard({
      holdings: mockHoldings,
      activities: mockActivities,
      performance: partialPerformance,
    });

    expect(
      screen.getByText(/Some prices are unavailable: TSLA/)
    ).toBeInTheDocument();
  });

  // ── Summary line ────────────────────────────────────────────────────────

  it("renders summary line with active symbols and total shares", () => {
    renderDashboard({
      holdings: mockHoldings,
      activities: mockActivities,
      performance: mockPerformance,
    });

    expect(screen.getByText("Active symbols: 2")).toBeInTheDocument();
    expect(
      screen.getByText(/Total shares across active holdings: 14/)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Portfolio base currency:/)
    ).toBeInTheDocument();
  });

  it("renders day-change fallback text when data is null", () => {
    const noDayChange: PortfolioPerformance = {
      ...mockPerformance,
      dayChange: null,
      dayChangePercentage: null,
    };

    renderDashboard({
      holdings: mockHoldings,
      activities: mockActivities,
      performance: noDayChange,
    });

    expect(
      screen.getByText("Previous-close data is currently unavailable.")
    ).toBeInTheDocument();
  });
});
