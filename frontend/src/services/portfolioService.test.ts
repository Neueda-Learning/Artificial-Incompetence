import {
  getPortfolioItems,
  createPortfolioItem,
  updatePortfolioItemQuantity,
  deletePortfolioItem,
  getPortfolioPerformance,
  getHistoricalPerformance,
  getPortfolioActivities,
  getTransactions,
  createTransaction,
} from "./portfolioService";
import api from "./api";

jest.mock("./api", () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
}));

const mockedApi = api as jest.Mocked<typeof api>;

beforeEach(() => {
  jest.clearAllMocks();
});

// Ensure we are NOT in mock data mode
const originalEnv = process.env;
beforeAll(() => {
  process.env = { ...originalEnv, REACT_APP_USE_MOCK_DATA: "false" };
});
afterAll(() => {
  process.env = originalEnv;
});

describe("portfolioService", () => {
  describe("getPortfolioItems", () => {
    it("fetches portfolio items and normalizes them", async () => {
      const rawItems = [
        {
          id: 1,
          assetType: "STOCK",
          symbol: "AAPL",
          companyName: "Apple Inc.",
          exchange: "NASDAQ",
          quantity: 10,
          currency: "USD",
        },
      ];
      (mockedApi.get as jest.Mock).mockResolvedValue({ data: rawItems });

      const result = await getPortfolioItems();

      expect(result).toHaveLength(1);
      expect(result[0].symbol).toBe("AAPL");
      expect(result[0].quantity).toBe(10);
      expect(mockedApi.get).toHaveBeenCalledWith("/portfolio/items");
    });

    it("propagates error on network failure", async () => {
      (mockedApi.get as jest.Mock).mockRejectedValue(new Error("Network error"));

      await expect(getPortfolioItems()).rejects.toThrow("Network error");
    });
  });

  describe("createPortfolioItem", () => {
    it("creates a portfolio item", async () => {
      const rawItem = {
        id: 1,
        assetType: "STOCK",
        symbol: "AAPL",
        companyName: "Apple Inc.",
        exchange: "NASDAQ",
        quantity: 10,
        currency: "USD",
      };
      (mockedApi.post as jest.Mock).mockResolvedValue({ data: rawItem });

      const result = await createPortfolioItem({
        assetType: "STOCK",
        symbol: "AAPL",
        quantity: 10,
      });

      expect(result.symbol).toBe("AAPL");
      expect(result.quantity).toBe(10);
      expect(mockedApi.post).toHaveBeenCalledWith("/portfolio/items", {
        assetType: "STOCK",
        symbol: "AAPL",
        quantity: 10,
      });
    });
  });

  describe("updatePortfolioItemQuantity", () => {
    it("updates quantity of a portfolio item", async () => {
      const rawItem = {
        id: 20,
        assetType: "STOCK",
        symbol: "AAPL",
        companyName: "Apple Inc.",
        exchange: "NASDAQ",
        quantity: 6,
        currency: "USD",
      };
      (mockedApi.put as jest.Mock).mockResolvedValue({ data: rawItem });

      const result = await updatePortfolioItemQuantity(20, 6);

      expect(result.quantity).toBe(6);
      expect(mockedApi.put).toHaveBeenCalledWith("/portfolio/items/20/quantity", {
        quantity: 6,
      });
    });
  });

  describe("deletePortfolioItem", () => {
    it("deletes a portfolio item", async () => {
      (mockedApi.delete as jest.Mock).mockResolvedValue({});

      await deletePortfolioItem(42);

      expect(mockedApi.delete).toHaveBeenCalledWith("/portfolio/items/42");
    });
  });

  describe("getPortfolioPerformance", () => {
    it("fetches and normalizes portfolio performance", async () => {
      const rawPerformance = {
        currency: "USD",
        totalCost: 1000.0,
        currentValue: 1200.0,
        unrealizedProfitLoss: 200.0,
        returnPercentage: 20.0,
        dayChange: 50.0,
        dayChangePercentage: 4.17,
        status: "COMPLETE",
        priceUpdatedAt: "2026-07-27T10:30:00Z",
        assets: [
          {
            symbol: "AAPL",
            quantity: 10,
            averageCost: 100.0,
            currentPrice: 120.0,
            costBasis: 1000.0,
            currentValue: 1200.0,
            unrealizedProfitLoss: 200.0,
            returnPercentage: 20.0,
            allocationPercentage: 100.0,
          },
        ],
        missingPrices: [],
      };
      (mockedApi.get as jest.Mock).mockResolvedValue({ data: rawPerformance });

      const result = await getPortfolioPerformance();

      expect(result.status).toBe("COMPLETE");
      expect(result.totalCost).toBe(1000);
      expect(result.currentValue).toBe(1200);
      expect(result.unrealizedProfitLoss).toBe(200);
      expect(result.assets).toHaveLength(1);
      expect(result.assets[0].averageCost).toBe(100);
      expect(mockedApi.get).toHaveBeenCalledWith("/portfolio/performance");
    });

    it("propagates error on network failure", async () => {
      (mockedApi.get as jest.Mock).mockRejectedValue(new Error("Network error"));

      await expect(getPortfolioPerformance()).rejects.toThrow("Network error");
    });
  });

  describe("getHistoricalPerformance", () => {
    it("fetches historical performance for a given range", async () => {
      const rawHistory = {
        currency: "USD",
        range: "1M",
        startDate: "2026-06-27",
        endDate: "2026-07-27",
        status: "COMPLETE",
        points: [
          {
            date: "2026-06-27",
            marketValue: 1000,
            costBasis: 900,
            profitLoss: 100,
            returnPercentage: 11.11,
          },
        ],
        assets: [],
        missingData: [],
      };
      (mockedApi.get as jest.Mock).mockResolvedValue({ data: rawHistory });

      const result = await getHistoricalPerformance("1M");

      expect(result.range).toBe("1M");
      expect(result.points).toHaveLength(1);
      expect(mockedApi.get).toHaveBeenCalledWith(
        "/portfolio/performance/history",
        { params: { range: "1M" } }
      );
    });

    it("propagates error on network failure", async () => {
      (mockedApi.get as jest.Mock).mockRejectedValue(new Error("Network error"));

      await expect(getHistoricalPerformance("1M")).rejects.toThrow("Network error");
    });
  });

  describe("getPortfolioActivities", () => {
    it("fetches and maps activity records", async () => {
      const rawActivities = [
        {
          id: 1,
          action: "ADDED",
          assetType: "STOCK",
          symbol: "AAPL",
          quantity: 10,
          pricePerUnit: 195.0,
          currency: "USD",
          remainingQuantity: null,
          occurredAt: "2026-07-27T10:30:00Z",
        },
      ];
      (mockedApi.get as jest.Mock).mockResolvedValue({ data: rawActivities });

      const result = await getPortfolioActivities();

      expect(result).toHaveLength(1);
      expect(result[0].action).toBe("ADDED");
      expect(result[0].symbol).toBe("AAPL");
      expect(result[0].shares).toBe(10);
      expect(result[0].date).toBe("2026-07-27T10:30:00Z");
      expect(mockedApi.get).toHaveBeenCalledWith("/portfolio/activities");
    });

    it("throws on network error via requestError wrapper", async () => {
      (mockedApi.get as jest.Mock).mockRejectedValue(new Error("Network error"));

      await expect(getPortfolioActivities()).rejects.toThrow("Network error");
    });
  });

  describe("getTransactions", () => {
    it("fetches transactions with BUY type", async () => {
      (mockedApi.get as jest.Mock).mockResolvedValue({ data: [] });

      const result = await getTransactions();

      expect(result).toEqual([]);
      expect(mockedApi.get).toHaveBeenCalledWith("/transactions", {
        params: { type: "BUY" },
      });
    });
  });

  describe("createTransaction", () => {
    it("creates a buy transaction", async () => {
      const rawTransaction = {
        id: 1,
        assetType: "STOCK",
        symbol: "AAPL",
        quantity: 5,
        pricePerUnit: 195.0,
        currency: "USD",
        purchasedAt: "2026-07-27T10:30:00Z",
      };
      (mockedApi.post as jest.Mock).mockResolvedValue({ data: rawTransaction });

      const result = await createTransaction({
        transactionType: "BUY" as const,
        assetType: "STOCK",
        symbol: "AAPL",
        quantity: 5,
        pricePerUnit: 195.0,
        purchasedAt: "2026-07-27T10:30:00Z",
      });

      expect(result.symbol).toBe("AAPL");
      expect(result.quantity).toBe(5);
      expect(mockedApi.post).toHaveBeenCalledWith("/transactions", {
        transactionType: "BUY",
        assetType: "STOCK",
        symbol: "AAPL",
        quantity: 5,
        pricePerUnit: 195.0,
        purchasedAt: "2026-07-27T10:30:00Z",
      });
    });

    it("throws error with field errors from API response", async () => {
      const errorResponse = {
        isAxiosError: true,
        response: {
          data: {
            message: "Request validation failed",
            fieldErrors: {
              quantity: "must be greater than 0",
              symbol: "must not be blank",
            },
          },
        },
      };
      (mockedApi.post as jest.Mock).mockRejectedValue(errorResponse);

      await expect(
        createTransaction({
          transactionType: "BUY" as const,
          assetType: "STOCK",
          symbol: "",
          quantity: 0,
          pricePerUnit: 195.0,
          purchasedAt: "2026-07-27T10:30:00Z",
        })
      ).rejects.toThrow("must be greater than 0");
    });
  });
});
