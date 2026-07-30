import React from "react";
import { useMemo, useState } from "react";
import { AggregatedHolding } from "../../types/portfolio";
import { formatNumber } from "../../utils/formatters";

export interface RemoveAssetPayload {
  symbol: string;
  shares: number;
}

interface DeleteAssetModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (payload: RemoveAssetPayload) => Promise<string>;
  holdings: AggregatedHolding[];
}

/**
 * 中文：让用户选择当前持仓和删除数量，并通过二次确认降低误删风险。
 * English: Lets users choose an active holding and removal quantity with a confirmation step.
 */
function DeleteAssetModal({
  isOpen,
  onClose,
  onSubmit,
  holdings,
}: DeleteAssetModalProps) {
  const [symbol, setSymbol] = useState("");
  const [shares, setShares] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isConfirming, setIsConfirming] = useState(false);

  const selectedHolding = useMemo(
    () => holdings.find((holding) => holding.symbol === symbol) ?? null,
    [symbol, holdings],
  );

  const sharesValue = Number(shares);
  const remainingShares = selectedHolding
    ? Number(
        (
          selectedHolding.quantity -
          (Number.isFinite(sharesValue) ? sharesValue : 0)
        ).toFixed(8),
      )
    : null;

  // 中文：验证删除数量必须有效、为正数且不能超过当前持仓。
  // English: Validates that the removal quantity is valid, positive, and no greater than the holding.
  const validationError = useMemo(() => {
    if (!symbol) {
      return "Please choose an active asset.";
    }
    if (!selectedHolding) {
      return "This asset is no longer active.";
    }
    if (!Number.isFinite(sharesValue) || sharesValue <= 0) {
      return "Removal quantity must be greater than zero.";
    }
    if (sharesValue > selectedHolding.quantity) {
      return "Removal quantity cannot exceed current shares.";
    }
    return null;
  }, [selectedHolding, sharesValue, symbol]);

  if (!isOpen) {
    return null;
  }

  /**
   * 中文：清空删除表单及其提示状态。
   * English: Clears the removal form and all feedback state.
   */
  const resetForm = () => {
    setSymbol("");
    setShares("");
    setError(null);
    setSuccess(null);
    setIsConfirming(false);
  };

  /**
   * 中文：关闭弹窗前重置表单，确保下次打开不会残留旧数据。
   * English: Resets the form before closing so stale values do not appear next time.
   */
  const handleClose = () => {
    resetForm();
    onClose();
  };

  /**
   * 中文：完成第一阶段校验并进入最终确认状态，不立即修改数据。
   * English: Runs first-stage validation and opens confirmation without changing data.
   */
  const handleInitialSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    if (validationError) {
      setError(validationError);
      return;
    }
    setIsConfirming(true);
  };

  /**
   * 中文：最终确认后调用父组件删除逻辑，并显示后端同步结果。
   * English: Invokes the parent removal flow after final confirmation and displays the synchronization result.
   */
  const handleFinalConfirm = async () => {
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const message = await onSubmit({
        symbol,
        shares: sharesValue,
      });
      setSuccess(message);
      setIsConfirming(false);
      setShares("");
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Unable to remove asset.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" role="presentation" onClick={handleClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="remove-asset-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <h2 id="remove-asset-title">Remove Asset</h2>
          <button
            type="button"
            className="icon-button"
            onClick={handleClose}
            aria-label="Close Remove Asset"
          >
            ×
          </button>
        </div>

        {holdings.length === 0 ? (
          <div className="empty-block">
            <p>No active positions are available to remove.</p>
          </div>
        ) : (
          <form onSubmit={handleInitialSubmit} className="modal-form">
            <label>
              Asset
              <select
                value={symbol}
                onChange={(event) => setSymbol(event.target.value)}
                required
              >
                <option value="">Select an active position</option>
                {holdings.map((holding) => (
                  <option key={holding.symbol} value={holding.symbol}>
                    {holding.symbol} · {formatNumber(holding.quantity, 4)}{" "}
                    shares
                  </option>
                ))}
              </select>
            </label>
            <label>
              Shares to remove
              <input
                required
                type="number"
                step="0.0001"
                min="0.0001"
                value={shares}
                onChange={(event) => setShares(event.target.value)}
              />
            </label>

            {selectedHolding && (
              <div className="info-callout" role="status">
                <p>
                  Current shares: {formatNumber(selectedHolding.quantity, 4)}
                </p>
                <p>
                  Remaining shares:{" "}
                  {remainingShares !== null && remainingShares >= 0
                    ? formatNumber(remainingShares, 4)
                    : "—"}
                </p>
                {remainingShares !== null && remainingShares > 0 && (
                  <p>
                    {selectedHolding.symbol} will remain in your portfolio and
                    continue receiving updates.
                  </p>
                )}
                {remainingShares === 0 && (
                  <p>
                    {selectedHolding.symbol} will be removed from current
                    holdings. Its transaction history will also be permanently
                    deleted.
                  </p>
                )}
              </div>
            )}

            {isConfirming && !validationError && (
              <div className="confirm-callout">
                <p>Please confirm this removal action.</p>
                <button
                  type="button"
                  className="button button-danger"
                  onClick={handleFinalConfirm}
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "Removing…" : "Confirm Remove Asset"}
                </button>
              </div>
            )}

            {error && <p className="banner banner-error">{error}</p>}
            {success && <p className="banner banner-success">{success}</p>}

            <div className="modal-actions">
              <button
                type="button"
                className="button button-ghost"
                onClick={handleClose}
                disabled={isSubmitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="button button-secondary"
                disabled={isSubmitting}
              >
                Review Remove
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

export default DeleteAssetModal;
