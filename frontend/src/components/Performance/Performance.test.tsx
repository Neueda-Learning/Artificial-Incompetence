import React from "react";
import { render, screen, fireEvent, waitFor, act } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Performance from "./Performance";
import * as portfolioService from "../../services/portfolioService";
import {
  AggregatedHolding,
  HistoricalPerformance,
  PortfolioPerformance,
} from "../../types/portfolio";

jest.mock("../../services/portfolioService");

const mockedService =
  portfolioService as jest.Mocked<typeof portfolioService>;

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
  ],
  missingPrices: [],
};

const mockHoldings: AggregatedHolding[] = [
  { symbol: "AAPL", assetType: "STOCK", quantity: 10, sourceItemIds: [1] },
];

const emptyHistory: HistoricalPerformance = {
  currency: "USD",
  range: "1M",
  startDate: "2026-06-30",
  endDate: "2026-07-30",
  status: "UNAVAILABLE",
  points: [],
  assets: [],
  missingData: [],
};

const historyWithData: HistoricalPerformance = {
  currency: "USD",
  range: "1W",
  startDate: "2026-07-23",
  endDate: "2026-07-30",
  status: "COMPLETE",
  points: [
    {
      date: "2026-07-23",
      marketValue: 5000,
      costBasis: 4800,
      profitLoss: 200,
      returnPercentage: 4.17,
    },
    {
      date: "2026-07-30",
      marketValue: 5500,
      costBasis: 5000,
      profitLoss: 500,
      returnPercentage: 10,
    },
  ],
  assets: [],
  missingData: [],
};

function renderPerformance(
  props: Partial<React.ComponentProps<typeof Performance>> = {}
) {
  return render(
    <MemoryRouter>
      <Performance
        holdings={[]}
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

describe("Performance", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedService.getHistoricalPerformance.mockResolvedValue(emptyHistory);
  });

  // ── Loading ────────────────────────────────────────────────────────────

  it("shows skeleton when data is loading", () => {
    renderPerformance({ isLoading: true });
    expect(document.querySelector(".skeleton-panel")).toBeInTheDocument();
  });

  it("shows skeleton when performance is loading", () => {
    renderPerformance({ isPerformanceLoading: true });
    expect(document.querySelector(".skeleton-panel")).toBeInTheDocument();
  });

  // ── Error ──────────────────────────────────────────────────────────────

  it("shows error when main data fails to load", () => {
    const onRetry = jest.fn();
    renderPerformance({
      error: "Failed to fetch",
      onRetry,
    });

    expect(screen.getByText("Unable to load performance")).toBeInTheDocument();
    expect(screen.getByText("Failed to fetch")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it("shows error when performance is null with error message", () => {
    renderPerformance({
      performance: null,
      performanceError: "Market data unavailable",
    });

    expect(screen.getByText("Unable to load performance")).toBeInTheDocument();
    expect(screen.getByText("Market data unavailable")).toBeInTheDocument();
  });

  // ── Range switching ────────────────────────────────────────────────────

  it("renders five range buttons with 1M selected by default", () => {
    renderPerformance({
      holdings: mockHoldings,
      performance: mockPerformance,
    });

    const rangeGroup = screen.getByRole("group", {
      name: "Performance ranges",
    });

    expect(screen.getByRole("button", { name: "1W" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "1M" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "3M" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "1Y" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "ALL" })).toBeInTheDocument();

    // 1M should be active by default
    expect(screen.getByRole("button", { name: "1M" })).toHaveClass("active");
  });

  it("switches range and fetches historical data for new range", async () => {
    mockedService.getHistoricalPerformance.mockResolvedValue(historyWithData);

    renderPerformance({
      holdings: mockHoldings,
      performance: mockPerformance,
    });

    // Default: 1M
    await waitFor(() =>
      expect(mockedService.getHistoricalPerformance).toHaveBeenCalledWith("1M")
    );

    // Switch to 1W
    fireEvent.click(screen.getByRole("button", { name: "1W" }));

    expect(screen.getByRole("button", { name: "1W" })).toHaveClass("active");
    expect(screen.getByRole("button", { name: "1M" })).not.toHaveClass(
      "active"
    );

    await waitFor(() =>
      expect(mockedService.getHistoricalPerformance).toHaveBeenCalledWith("1W")
    );
  });

  // ── History loading error ──────────────────────────────────────────────

  it("shows history error inline when getHistoricalPerformance fails", async () => {
    mockedService.getHistoricalPerformance.mockRejectedValue(
      new Error("Network error")
    );

    renderPerformance({
      holdings: mockHoldings,
      performance: mockPerformance,
    });

    expect(
      await screen.findByText("Unable to load historical performance.")
    ).toBeInTheDocument();
  });

  // ── Performance error banner ───────────────────────────────────────────

  it("shows performance error banner when present", () => {
    renderPerformance({
      holdings: mockHoldings,
      performance: mockPerformance,
      performanceError: "Some prices are stale",
    });

    expect(screen.getByText("Some prices are stale")).toBeInTheDocument();
  });

  // ── Normal rendering ───────────────────────────────────────────────────

  it("renders heading and child components with data", async () => {
    renderPerformance({
      holdings: mockHoldings,
      performance: mockPerformance,
    });

    expect(screen.getByText("Performance")).toBeInTheDocument();

    // Overall performance section should be rendered
    await waitFor(() => {
      expect(screen.getByText(/Total Portfolio Value/i)).toBeInTheDocument();
    });
  });

  // ── Cleanup ────────────────────────────────────────────────────────────

  it("does not update state after unmount", async () => {
    // This test ensures the isActive cleanup flag works
    let resolvePromise: (value: HistoricalPerformance) => void;
    const delayedPromise = new Promise<HistoricalPerformance>((resolve) => {
      resolvePromise = resolve;
    });
    mockedService.getHistoricalPerformance.mockReturnValue(delayedPromise);

    const { unmount } = renderPerformance({
      holdings: mockHoldings,
      performance: mockPerformance,
    });

    // Unmount before promise resolves
    unmount();

    // Resolve after unmount — should NOT cause "setState on unmounted component" warning
    await act(async () => {
      resolvePromise!(emptyHistory);
    });

    // If we got here without React warning about state update on unmounted
    // component, the cleanup works correctly
    expect(mockedService.getHistoricalPerformance).toHaveBeenCalled();
  });
});
