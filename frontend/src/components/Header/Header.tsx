import React from "react";
import { NavLink } from "react-router-dom";
import { formatDateTime } from "../../utils/formatters";

interface HeaderProps {
  onAddAsset: () => void;
  onRemoveAsset: () => void;
  lastUpdated: string | null;
  staleWarning: string | null;
}

function Header({
  onAddAsset,
  onRemoveAsset,
  lastUpdated,
  staleWarning,
}: HeaderProps) {
  return (
    <header className="app-header">
      <div className="header-row">
        <div>
          <p className="eyebrow">Portfolio Manager</p>
          <h1 className="page-title">Portfolio Manager</h1>
          <p className="subtle-text">
            Base currency: USD · Last updated: {formatDateTime(lastUpdated)}
          </p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="button button-primary"
            onClick={onAddAsset}
          >
            Add Asset
          </button>
          <button
            type="button"
            className="button button-secondary"
            onClick={onRemoveAsset}
          >
            Remove Asset
          </button>
        </div>
      </div>
      <nav className="primary-nav" aria-label="Primary">
        <NavLink
          to="/dashboard"
          className={({ isActive }) =>
            isActive ? "nav-link active" : "nav-link"
          }
        >
          Dashboard
        </NavLink>
        <NavLink
          to="/holdings"
          className={({ isActive }) =>
            isActive ? "nav-link active" : "nav-link"
          }
        >
          Holdings
        </NavLink>
        <NavLink
          to="/performance"
          className={({ isActive }) =>
            isActive ? "nav-link active" : "nav-link"
          }
        >
          Performance
        </NavLink>
      </nav>
      {staleWarning && <p className="banner banner-warning">{staleWarning}</p>}
    </header>
  );
}

export default Header;
