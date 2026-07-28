import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import App from "./App";
import * as portfolioService from "./services/portfolioService";
import {
  HistoricalPerformance,
  PortfolioPerformance,
} from "./types/portfolio";

jest.mock("./services/portfolioService");

const mockedService = portfolioService as jest.Mocked<typeof portfolioService>;

const emptyPerformance: PortfolioPerformance = {
  currency: "USD",
  totalCost: 0,
  currentValue: 0,
  unrealizedProfitLoss: 0,
  returnPercentage: 0,
  status: "COMPLETE",
  priceUpdatedAt: null,
  assets: [],
  missingPrices: [],
};

const emptyHistory: HistoricalPerformance = {
  currency: "USD",
  range: "1M",
  startDate: "2026-06-28",
  endDate: "2026-07-28",
  status: "UNAVAILABLE",
  points: [],
  missingData: [],
};

describe("Portfolio Manager frontend", () => {
  beforeEach(() => {
    window.history.pushState({}, "", "/dashboard");
    window.localStorage.clear();
    jest.clearAllMocks();
    mockedService.getPortfolioItems.mockResolvedValue([]);
    mockedService.getPortfolioPerformance.mockResolvedValue(emptyPerformance);
    mockedService.getHistoricalPerformance.mockResolvedValue(emptyHistory);
    mockedService.createPortfolioItem.mockResolvedValue({
      id: 1,
      assetType: "STOCK",
      symbol: "AAPL",
      quantity: 5,
    });
    mockedService.deletePortfolioItem.mockResolvedValue();
  });

  it("shows first-visit onboarding empty state", async () => {
    render(<App />);

    expect(
      await screen.findByText("Start building your portfolio"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Add your first asset" }),
    ).toBeInTheDocument();
  });

  it("shows existing-symbol hint in Add Asset flow", async () => {
    mockedService.getPortfolioItems.mockResolvedValueOnce([
      { id: 10, assetType: "STOCK", symbol: "AAPL", quantity: 10 },
    ]);

    render(<App />);
    await screen.findByText("Top Holdings");

    fireEvent.click(screen.getByRole("button", { name: "Add Asset" }));
    fireEvent.change(screen.getByLabelText("Asset symbol"), {
      target: { value: "AAPL" },
    });

    expect(
      await screen.findByText(/is already in your portfolio/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/Current shares: 10/i)).toBeInTheDocument();
  });

  it("shows partial-removal guidance in Remove Asset flow", async () => {
    mockedService.getPortfolioItems.mockResolvedValueOnce([
      { id: 20, assetType: "STOCK", symbol: "AAPL", quantity: 10 },
    ]);

    render(<App />);
    await screen.findByText("Top Holdings");

    fireEvent.click(screen.getAllByRole("button", { name: "Remove Asset" })[0]);
    fireEvent.change(screen.getByLabelText("Asset"), {
      target: { value: "AAPL" },
    });
    fireEvent.change(screen.getByLabelText("Shares to remove"), {
      target: { value: "4" },
    });

    expect(screen.getByText(/Remaining shares: 6/i)).toBeInTheDocument();
    expect(
      screen.getByText(/will remain in your portfolio/i),
    ).toBeInTheDocument();
  });

  it("shows full-removal guidance in Remove Asset flow", async () => {
    mockedService.getPortfolioItems.mockResolvedValueOnce([
      { id: 21, assetType: "STOCK", symbol: "AAPL", quantity: 10 },
    ]);

    render(<App />);
    await screen.findByText("Top Holdings");

    fireEvent.click(screen.getAllByRole("button", { name: "Remove Asset" })[0]);
    fireEvent.change(screen.getByLabelText("Asset"), {
      target: { value: "AAPL" },
    });
    fireEvent.change(screen.getByLabelText("Shares to remove"), {
      target: { value: "10" },
    });

    expect(screen.getByText(/Remaining shares: 0/i)).toBeInTheDocument();
    expect(
      screen.getByText(/will be removed from current holdings/i),
    ).toBeInTheDocument();
  });

  it("aggregates repeated symbols into one active row", async () => {
    mockedService.getPortfolioItems.mockResolvedValueOnce([
      { id: 1, assetType: "STOCK", symbol: "AAPL", quantity: 3 },
      { id: 2, assetType: "STOCK", symbol: "AAPL", quantity: 2 },
    ]);

    render(<App />);
    await screen.findByText("Top Holdings");
    fireEvent.click(screen.getByRole("link", { name: "Holdings" }));

    const rows = await screen.findAllByRole("row");
    expect(rows.length).toBeGreaterThan(1);
    expect(screen.getAllByText("AAPL").length).toBe(1);
    expect(screen.getByText("5")).toBeInTheDocument();
  });

  it("shows local history when no current holdings remain", async () => {
    window.localStorage.setItem(
      "portfolio-manager-activity-v1",
      JSON.stringify([
        {
          id: "x1",
          action: "ADDED",
          symbol: "AAPL",
          shares: 5,
          date: "2026-07-20T00:00:00.000Z",
        },
      ]),
    );

    render(<App />);
    expect(await screen.findByText("No current holdings")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("link", { name: "View History" }));
    expect(
      await screen.findByRole("heading", { name: "History" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Added")).toBeInTheDocument();
  });

  it("handles loading and error states", async () => {
    mockedService.getPortfolioItems.mockImplementationOnce(
      () => new Promise((resolve) => setTimeout(() => resolve([]), 30)),
    );
    render(<App />);
    expect(document.querySelector(".skeleton-panel")).toBeInTheDocument();

    await waitFor(() => {
      expect(
        screen.getByText("Start building your portfolio"),
      ).toBeInTheDocument();
    });
  });

  it("shows explicit USD references in summary and native-price labels in positions", async () => {
    mockedService.getPortfolioItems.mockResolvedValueOnce([
      { id: 30, assetType: "STOCK", symbol: "AAPL", quantity: 2 },
    ]);
    render(<App />);
    await screen.findByText("Top Holdings");

    expect(screen.getByText(/Base currency: USD/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("link", { name: "Holdings" }));
    expect(await screen.findByText("Current Price")).toBeInTheDocument();
    expect(screen.getByText("Avg. Cost")).toBeInTheDocument();
  });

  it("renders backend performance values in holdings", async () => {
    mockedService.getPortfolioItems.mockResolvedValueOnce([
      { id: 40, assetType: "STOCK", symbol: "MSFT", quantity: 10 },
    ]);
    mockedService.getPortfolioPerformance.mockResolvedValueOnce({
      ...emptyPerformance,
      totalCost: 4000,
      currentValue: 3891,
      unrealizedProfitLoss: -109,
      returnPercentage: -2.725,
      assets: [
        {
          symbol: "MSFT",
          quantity: 10,
          averageCost: 400,
          currentPrice: 389.1,
          costBasis: 4000,
          currentValue: 3891,
          unrealizedProfitLoss: -109,
          returnPercentage: -2.725,
          allocationPercentage: 100,
        },
      ],
    });

    render(<App />);
    await screen.findByText("Top Holdings");
    fireEvent.click(screen.getByRole("link", { name: "Holdings" }));

    expect(await screen.findByText("USD 389.10")).toBeInTheDocument();
    expect(screen.getByText("USD 400.00")).toBeInTheDocument();
    expect(screen.getByText("USD 3,891.00")).toBeInTheDocument();
    expect(screen.getByText("−USD 109.00")).toBeInTheDocument();
  });
});
