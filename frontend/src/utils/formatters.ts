export function formatNumber(value: number, maximumFractionDigits = 2): string {
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 0,
    maximumFractionDigits,
  }).format(value);
}

export function formatUsd(value?: number | null): string {
  if (value === null || value === undefined) {
    return "—";
  }

  return `USD ${new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)}`;
}

export function formatSignedUsd(value?: number | null): string {
  if (value === null || value === undefined) {
    return "—";
  }

  const sign = value >= 0 ? "+" : "−";
  const amount = Math.abs(value);
  return `${sign}${formatUsd(amount)}`;
}

export function formatPercent(value?: number | null): string {
  if (value === null || value === undefined) {
    return "—";
  }

  const sign = value >= 0 ? "+" : "−";
  return `${sign}${Math.abs(value).toFixed(2)}%`;
}

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
