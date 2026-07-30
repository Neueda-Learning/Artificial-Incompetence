import React from "react";
import { ActivityRecord } from "../../types/portfolio";
import { formatDate, formatNumber } from "../../utils/formatters";

interface PurchaseHistoryProps {
  activities: ActivityRecord[];
}

function PurchaseHistory({ activities }: PurchaseHistoryProps) {
  return (
    <article className="panel">
      <h3>History</h3>
      {activities.length === 0 ? (
        <p className="subtle-text">
          No purchase or removal history is available yet.
        </p>
      ) : (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Date</th>
                <th scope="col">Action</th>
                <th scope="col">Asset</th>
                <th scope="col" className="numeric-cell">
                  Shares
                </th>
                <th scope="col" className="numeric-cell">
                  Price
                </th>
                <th scope="col" className="numeric-cell">
                  Remaining Shares
                </th>
              </tr>
            </thead>
            <tbody>
              {activities.map((record) => (
                <tr key={record.id}>
                  <td>{formatDate(record.date)}</td>
                  <td>{record.action === "ADDED" ? "Added" : "Removed"}</td>
                  <td>{record.symbol}</td>
                  <td className="numeric-cell financial-value">
                    {formatNumber(record.shares, 4)}
                  </td>
                  <td className="numeric-cell financial-value">
                    {record.pricePerUnit == null
                      ? "—"
                      : `${record.currency ?? ""} ${record.pricePerUnit.toFixed(2)}`.trim()}
                  </td>
                  <td className="numeric-cell financial-value">
                    {record.remainingShares ?? "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </article>
  );
}

export default PurchaseHistory;
