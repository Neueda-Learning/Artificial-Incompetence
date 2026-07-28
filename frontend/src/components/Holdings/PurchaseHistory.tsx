import React from "react";
import { ActivityRecord, Transaction } from "../../types/portfolio";
import { formatDate, formatNumber, formatUsd } from "../../utils/formatters";

interface PurchaseHistoryProps {
  activities: ActivityRecord[];
  transactions: Transaction[];
}

function PurchaseHistory({ activities, transactions }: PurchaseHistoryProps) {
  const historyRecords = [
    ...transactions.map((transaction) => ({
      id: `tx-${transaction.id}`,
      date: transaction.purchasedAt,
      action: "Added",
      symbol: transaction.symbol,
      shares: transaction.quantity,
      price: transaction.pricePerUnit,
      remainingShares: null as number | null,
    })),
    ...activities.map((activity) => ({
      id: `local-${activity.id}`,
      date: activity.date,
      action: activity.action === "ADDED" ? "Added" : "Removed",
      symbol: activity.symbol,
      shares: activity.shares,
      price: null as number | null,
      remainingShares: activity.remainingShares ?? null,
    })),
  ].sort((a, b) => +new Date(b.date) - +new Date(a.date));

  return (
    <article className="panel">
      <h3>History</h3>
      {historyRecords.length === 0 ? (
        <p className="subtle-text">
          No Add or Remove history is available yet.
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
              {historyRecords.map((record) => (
                <tr key={record.id}>
                  <td>{formatDate(record.date)}</td>
                  <td>{record.action}</td>
                  <td>{record.symbol}</td>
                  <td className="numeric-cell financial-value">
                    {formatNumber(record.shares, 4)}
                  </td>
                  <td className="numeric-cell financial-value">
                    {formatUsd(record.price)}
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
