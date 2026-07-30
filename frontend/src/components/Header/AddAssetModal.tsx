import React from "react";
import { useMemo, useState } from "react";
import { AggregatedHolding, AssetType } from "../../types/portfolio";
import { formatNumber } from "../../utils/formatters";

export interface AddAssetPayload {
  assetType: AssetType;
  symbol: string;
  shares: number;
  purchaseDate: string;
  purchasePrice: number;
  currency?: string;
}

interface AddAssetModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (payload: AddAssetPayload) => Promise<string>;
  holdings: AggregatedHolding[];
}

const ASSET_TYPES: AssetType[] = ["STOCK", "BOND", "CASH"];

function todayAsInputValue(): string {
  const now = new Date();
  const localTime = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return localTime.toISOString().slice(0, 10);
}

function isFutureDate(dateValue: string): boolean {
  if (!dateValue) {
    return false;
  }
  const selectedDate = new Date(dateValue);
  const now = new Date();
  selectedDate.setHours(0, 0, 0, 0);
  now.setHours(0, 0, 0, 0);
  return selectedDate.getTime() > now.getTime();
}

function AddAssetModal({
  isOpen,
  onClose,
  onSubmit,
  holdings,
}: AddAssetModalProps) {
  const [assetType, setAssetType] = useState<AssetType>("STOCK");
  const [symbol, setSymbol] = useState("");
  const [shares, setShares] = useState("");
  const [purchaseDate, setPurchaseDate] = useState(todayAsInputValue);
  const [purchasePrice, setPurchasePrice] = useState("");
  const [currency, setCurrency] = useState("USD");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const normalizedSymbol = symbol.trim().toUpperCase();
  const currentHolding = useMemo(
    () => holdings.find((holding) => holding.symbol === normalizedSymbol),
    [holdings, normalizedSymbol],
  );

  const validationError = useMemo(() => {
    if (!normalizedSymbol) {
      return "Asset symbol is required.";
    }
    const sharesValue = Number(shares);
    if (!Number.isFinite(sharesValue) || sharesValue <= 0) {
      return "Shares must be greater than zero.";
    }
    if (!purchaseDate) {
      return "Purchase date is required.";
    }
    if (isFutureDate(purchaseDate)) {
      return "Purchase date cannot be in the future.";
    }
    const priceValue = Number(purchasePrice);
    if (!Number.isFinite(priceValue) || priceValue <= 0) {
      return "Purchase price must be greater than zero.";
    }
    if (
      assetType !== "STOCK" &&
      !/^[A-Za-z]{3}$/.test(currency.trim())
    ) {
      return "Currency must be a three-letter code such as USD or HKD.";
    }
    return null;
  }, [
    normalizedSymbol,
    shares,
    purchaseDate,
    purchasePrice,
    currency,
    assetType,
  ]);

  if (!isOpen) {
    return null;
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);
    try {
      const message = await onSubmit({
        assetType,
        symbol: normalizedSymbol,
        shares: Number(shares),
        purchaseDate,
        purchasePrice: Number(purchasePrice),
        currency:
          assetType === "STOCK"
            ? undefined
            : currency.trim().toUpperCase(),
      });
      setSuccess(message);
      setShares("");
      setPurchasePrice("");
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Unable to add asset.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="add-asset-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <h2 id="add-asset-title">Add Asset</h2>
          <button
            type="button"
            className="icon-button"
            onClick={onClose}
            aria-label="Close Add Asset"
          >
            ×
          </button>
        </div>
        <form onSubmit={handleSubmit} className="modal-form">
          <label>
            Asset symbol
            <input
              required
              name="symbol"
              type="text"
              maxLength={20}
              value={symbol}
              onChange={(event) => setSymbol(event.target.value.toUpperCase())}
              placeholder="AAPL or 0700"
            />
          </label>
          <label>
            Asset type
            <select
              value={assetType}
              onChange={(event) =>
                setAssetType(event.target.value as AssetType)
              }
            >
              {ASSET_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </label>
          <label>
            Shares
            <input
              required
              name="shares"
              type="number"
              step="0.0001"
              min="0.0001"
              value={shares}
              onChange={(event) => setShares(event.target.value)}
            />
          </label>
          {assetType !== "STOCK" && (
            <label>
              Trading currency
              <input
                required
                name="currency"
                type="text"
                minLength={3}
                maxLength={3}
                value={currency}
                onChange={(event) =>
                  setCurrency(event.target.value.toUpperCase())
                }
                placeholder="USD"
              />
            </label>
          )}
          <label>
            Purchase date
            <input
              required
              name="purchaseDate"
              type="date"
              value={purchaseDate}
              onChange={(event) => setPurchaseDate(event.target.value)}
            />
          </label>
          <label>
            Purchase price per share
            <input
              required
              name="purchasePrice"
              type="number"
              step="0.0001"
              min="0.0001"
              value={purchasePrice}
              onChange={(event) => setPurchasePrice(event.target.value)}
            />
          </label>

          <p className="helper-text">
            For stocks, enter a Twelve Data ticker only. Company name, exchange,
            and trading currency are detected automatically. Purchase details
            are saved as transaction history.
          </p>

          {currentHolding && (
            <div className="info-callout" role="status">
              <p>
                <strong>{normalizedSymbol}</strong> is already in your
                portfolio.
              </p>
              <p>Current shares: {formatNumber(currentHolding.quantity, 4)}</p>
              <p>
                This action increases the existing position instead of creating
                another active row.
              </p>
            </div>
          )}

          {error && <p className="banner banner-error">{error}</p>}
          {success && <p className="banner banner-success">{success}</p>}

          <div className="modal-actions">
            <button
              type="button"
              className="button button-ghost"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="button button-primary"
              disabled={isSubmitting || Boolean(validationError)}
            >
              {isSubmitting ? "Adding…" : "Add Asset"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddAssetModal;
