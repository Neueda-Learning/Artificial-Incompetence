/**
 * 中文：按美式数字格式显示数量，并允许调用方控制最大小数位。
 * English: Formats a number in the US locale with a configurable decimal limit.
 */
export function formatNumber(value: number, maximumFractionDigits = 2): string {
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 0,
    maximumFractionDigits,
  }).format(value);
}

/**
 * 中文：把金额格式化为 USD；没有数据时显示占位符。
 * English: Formats a monetary value as USD and shows a placeholder when unavailable.
 */
export function formatUsd(value?: number | null): string {
  if (value === null || value === undefined) {
    return "—";
  }

  return `USD ${new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)}`;
}

/**
 * 中文：格式化带正负号的美元盈亏金额。
 * English: Formats a USD profit/loss amount with an explicit sign.
 */
export function formatSignedUsd(value?: number | null): string {
  if (value === null || value === undefined) {
    return "—";
  }

  const sign = value >= 0 ? "+" : "−";
  const amount = Math.abs(value);
  return `${sign}${formatUsd(amount)}`;
}

/**
 * 中文：格式化带正负号的百分比收益率。
 * English: Formats a percentage return with an explicit sign.
 */
export function formatPercent(value?: number | null): string {
  if (value === null || value === undefined) {
    return "—";
  }

  const sign = value >= 0 ? "+" : "−";
  return `${sign}${Math.abs(value).toFixed(2)}%`;
}

/**
 * 中文：把 ISO 时间转换为页面使用的本地日期和时间。
 * English: Converts an ISO timestamp into the localized date-time shown in the UI.
 */
export function formatDateTime(iso?: string | null): string {
  if (!iso) {
    return "—";
  }

  return new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

/**
 * 中文：把 ISO 日期转换为页面使用的本地日期。
 * English: Converts an ISO value into the localized date shown in the UI.
 */
export function formatDate(iso?: string | null): string {
  if (!iso) {
    return "—";
  }
  return new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "short",
    day: "2-digit",
  }).format(new Date(iso));
}
