import {
  formatNumber,
  formatUsd,
  formatSignedUsd,
  formatPercent,
  formatDateTime,
  formatDate,
} from "./formatters";

describe("formatters", () => {
  describe("formatNumber", () => {
    it("formats whole numbers with 0 decimal places by default", () => {
      expect(formatNumber(1234)).toBe("1,234");
    });

    it("formats numbers with specified decimal places", () => {
      expect(formatNumber(1234.5678, 4)).toBe("1,234.5678");
    });

    it("defaults to 2 decimal places for fractional numbers", () => {
      expect(formatNumber(1234.5)).toBe("1,234.5");
      expect(formatNumber(1234.56)).toBe("1,234.56");
      expect(formatNumber(1234.567)).toBe("1,234.57");
    });

    it("rounds to specified max fraction digits", () => {
      expect(formatNumber(1234.5678, 3)).toBe("1,234.568");
    });

    it("handles zero", () => {
      expect(formatNumber(0)).toBe("0");
    });

    it("handles large numbers with commas", () => {
      expect(formatNumber(1_000_000)).toBe("1,000,000");
    });

    it("handles negative numbers", () => {
      expect(formatNumber(-1234.56)).toBe("-1,234.56");
    });
  });

  describe("formatUsd", () => {
    it("formats a number as USD", () => {
      expect(formatUsd(1234.56)).toBe("USD 1,234.56");
    });

    it("handles whole dollar amounts", () => {
      expect(formatUsd(100)).toBe("USD 100.00");
    });

    it("returns em-dash for null", () => {
      expect(formatUsd(null)).toBe("—");
    });

    it("returns em-dash for undefined", () => {
      expect(formatUsd(undefined)).toBe("—");
    });

    it("handles zero", () => {
      expect(formatUsd(0)).toBe("USD 0.00");
    });

    it("handles negative values", () => {
      expect(formatUsd(-50.5)).toBe("USD -50.50");
    });
  });

  describe("formatSignedUsd", () => {
    it("prepends plus sign for positive values", () => {
      expect(formatSignedUsd(1234.56)).toBe("+USD 1,234.56");
    });

    it("prepends minus sign for negative values", () => {
      expect(formatSignedUsd(-100)).toBe("−USD 100.00");
    });

    it("returns em-dash for null", () => {
      expect(formatSignedUsd(null)).toBe("—");
    });

    it("returns em-dash for undefined", () => {
      expect(formatSignedUsd(undefined)).toBe("—");
    });

    it("handles zero", () => {
      expect(formatSignedUsd(0)).toBe("+USD 0.00");
    });
  });

  describe("formatPercent", () => {
    it("formats positive percentage with plus sign", () => {
      expect(formatPercent(8.03)).toBe("+8.03%");
    });

    it("formats negative percentage with minus sign", () => {
      expect(formatPercent(-10.5)).toBe("−10.50%");
    });

    it("returns em-dash for null", () => {
      expect(formatPercent(null)).toBe("—");
    });

    it("returns em-dash for undefined", () => {
      expect(formatPercent(undefined)).toBe("—");
    });

    it("handles zero", () => {
      expect(formatPercent(0)).toBe("+0.00%");
    });
  });

  describe("formatDateTime", () => {
    it("formats an ISO datetime string", () => {
      const result = formatDateTime("2026-07-27T10:30:00Z");
      // Result depends on local timezone, but should contain date/time parts
      expect(result).toBeTruthy();
      expect(result).not.toBe("—");
    });

    it("returns em-dash for null", () => {
      expect(formatDateTime(null)).toBe("—");
    });

    it("returns em-dash for undefined", () => {
      expect(formatDateTime(undefined)).toBe("—");
    });
  });

  describe("formatDate", () => {
    it("formats an ISO date string", () => {
      const result = formatDate("2026-07-27");
      expect(result).toBeTruthy();
      expect(result).not.toBe("—");
      // Should contain month abbreviation and year
      expect(result).toMatch(/\d{4}/);
    });

    it("returns em-dash for null", () => {
      expect(formatDate(null)).toBe("—");
    });

    it("returns em-dash for undefined", () => {
      expect(formatDate(undefined)).toBe("—");
    });
  });
});
