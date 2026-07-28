---
name: portfolio-ui-design
description: Design, implement, and review a professional, trustworthy Portfolio Manager frontend. Use only for work inside the frontend directory, including portfolio summaries, holdings tables, performance and allocation charts, transaction flows, market-data states, financial data presentation, responsive layouts, accessibility, and financial UI copy. The portfolio base currency is fixed to USD. Do not use this skill for backend APIs, database models, persistence, market-data ingestion, exchange-rate logic, or server-side financial calculations.
---

# Portfolio UI Design

Act as a product designer and frontend engineer for a financial portfolio-management application. Create a distinctive but restrained interface whose primary qualities are correctness, clarity, consistency, accessibility, and trust.

This is an application dashboard, not a marketing landing page. Do not optimize for novelty at the expense of financial understanding.

## Scope

This skill applies only to frontend work inside the `frontend/` directory.

Use it for:

- React and TypeScript UI components
- Portfolio dashboard layouts
- Portfolio summary cards
- Holdings and transaction tables
- Performance and allocation charts
- Financial number and currency presentation
- Loading, empty, error, partial-data, and stale-data states
- Responsive design and accessibility
- User-facing financial UI copy

Do not use it to define or modify:

- Backend REST API behavior
- Database schemas or migrations
- Persistence logic
- Authentication or authorization
- Market-data ingestion
- Exchange-rate calculation logic
- Server-side portfolio, valuation, or profit-and-loss calculations
- Backend validation rules

The frontend must consume the backend data contract and must not invent,
duplicate, or silently change server-side financial logic.

## Priority order

When requirements conflict, use this order:

1. Data correctness
2. Financial clarity
3. Information hierarchy
4. Interaction consistency
5. Accessibility
6. Responsive usability
7. Visual polish
8. Distinctiveness

Never sacrifice a higher-priority requirement for a lower-priority one.

## Product context

The product helps users understand and manage a portfolio that may contain securities from multiple markets and currencies, including US, mainland China, and Hong Kong markets, etc..

The interface should help users answer these questions quickly:

- What is my portfolio worth in USD?
- How much did it gain or lose?
- Which holdings caused the change?
- How is the portfolio allocated?
- How fresh and complete is the data?
- What action can I take next?

Use the project's real domain language, routes, API fields, components, and data. Do not invent financial values merely to fill a layout. Clearly label mock or demo data when it is intentionally used.

## Fixed USD base currency

USD is the application's fixed base currency. Do not add a base-currency selector unless the product requirements are explicitly changed.

Apply these rules consistently:

- Use USD as the fixed portfolio base currency.
- Display total portfolio value in USD.
- Display portfolio-level gain and loss amounts in USD.
- Display each position's market value, total cost basis, and gain or loss in USD.
- Display portfolio performance time series in USD.
- Calculate and display allocation using USD-converted position market values.

- Display each security's quoted market price in its native market currency.
- Display average cost per share in the currency in which the security was acquired,
  normally the security's native trading currency.
- Clearly distinguish per-share values from total position values.

- Always show an explicit ISO currency code when the currency could be ambiguous,
  such as USD, CNY, or HKD.
- Never add monetary values from different currencies without conversion.
- Never label a converted USD value as though it were the security's native market quote.
- Show the exchange-rate timestamp used for USD-converted values.

Example holding presentation:

```text
0700.HK · HKEX
Current Price: HKD 421.80
Avg. Cost: HKD 390.00
Market Value: USD 5,394.62
Total Cost Basis: USD 4,987.96
Unrealized P/L: +USD 406.66
```

### FX data

- Show the exchange-rate timestamp wherever conversion freshness materially affects interpretation.
- Distinguish market-price freshness from FX-rate freshness.
- Keep full precision during calculations and round only for display.
- Do not silently reuse an expired or missing FX rate.
- When one or more holdings cannot be converted, label totals as partial or unavailable rather than presenting an apparently complete number.
- Identify which holdings are excluded from a partial total.

For historical performance, use the historical FX rate for each valuation date when available. If the implementation uses a current FX rate for historical values, label the chart as an estimate and disclose the methodology. Never present an approximation as exact performance.

## First viewport: a decision surface

Treat the first viewport as a decision surface rather than a promotional hero.

Users should immediately see:

- Total portfolio value in USD
- Absolute and percentage change for the selected period
- Selected time range
- Data freshness or delay
- Important missing-data or conversion warnings
- A clear path to view holdings or add a transaction

Avoid oversized decorative headlines, promotional copy, stock photography, or visual effects that compete with the financial information.

A large number with supporting statistics is acceptable when it is genuinely the most useful hierarchy, not because it is a default dashboard pattern.

## Visual direction

Give the product a deliberate visual identity, but keep the financial content calm and legible.

- Prefer a restrained palette with one controlled accent.
- Use generous but efficient spacing.
- Keep card, border, radius, elevation, and divider treatments consistent.
- Avoid excessive gradients, glassmorphism, neon trading-terminal effects, and decorative market imagery.
- Do not imitate a high-frequency trading terminal unless the brief explicitly requires it.
- A memorable signature element is optional. When used, it must encode real portfolio information rather than act as decoration.
- Spend visual boldness in one place and keep the rest disciplined.

## Typography and numbers

Typography carries hierarchy, but financial numbers prioritize legibility over personality.

- Use at most two font families unless an established design system requires more.
- Do not use decorative display fonts for balances, prices, percentages, dates, chart labels, or table data.
- Use tabular numerals for financial data.
- Right-align numeric columns in tables.
- Maintain consistent precision by data type.
- Apply thousands separators consistently.
- Use a true minus sign or a clearly consistent negative style.
- Keep currency codes and units visible at the point of interpretation.

Recommended CSS behavior:

```css
.financial-value,
.numeric-cell {
  font-variant-numeric: tabular-nums lining-nums;
}

.numeric-cell {
  text-align: right;
}
```

### Meaning of displayed values

- `USD 0.00` means the value is known and equal to zero.
- `—` means the value is unavailable or not applicable.
- `Partial` means a value was calculated from incomplete inputs.
- `Estimated` means a documented approximation was used.

Never substitute zero for missing data.

## Financial formatting

Use one consistent formatting utility rather than formatting values independently inside components.

Suggested defaults:

- Portfolio and position values: 2 decimal places
- Security prices: precision appropriate to the market or source
- Percentages: 2 decimal places
- Whole-share quantities: no unnecessary decimals
- Fractional quantities: preserve meaningful precision
- Dates and timestamps: include timezone when freshness matters

Use consistent signs:

```text
+USD 1,245.32
−USD 520.18
+2.34%
−1.12%
```

Pair positive and negative colors with a sign, arrow, icon, or text. Never rely on red and green alone.

## Information architecture

Structure is information. Dividers, labels, cards, tabs, and grouping must express real relationships.

A typical dashboard hierarchy may include:

1. Portfolio summary
2. Performance
3. Asset allocation
4. Holdings
5. Recent transactions or activity
6. Data status and warnings

Only use numbering when order is meaningful. Do not decorate sections with arbitrary `01 / 02 / 03` labels.

Keep vocabulary consistent across navigation, headings, buttons, API-backed states, and notifications. If the action is called `Add transaction`, use that same name throughout the flow.

## Asset lifecycle

- The product supports separate `Add Asset` and `Remove Asset` actions.
- Do not rename these actions to `Buy`, `Sell`, or `Add Transaction`.
- Adding an existing symbol increases its current share quantity rather than
  creating a duplicate position row.
- When the same symbol is added again, show the updated share quantity and
  weighted average cost returned by the backend.
- A partial removal reduces the current share quantity while keeping the
  position active.
- Continue displaying and refreshing market data while the remaining share
  quantity is greater than zero.
- When the remaining share quantity reaches zero, remove the position from
  current holdings and allocation, but preserve its activity history.
- Current holdings must contain at most one active row per symbol.
- Historical Add and Remove records must never be merged into the current
  position table.
- Do not calculate realized profit or loss in the frontend.
- Display realized profit or loss only when the backend explicitly provides it.

## Summary cards

Each summary card should answer one question and avoid duplicating nearby information.

For total portfolio value, include:

- Current USD value
- Period change in USD
- Period change in percent
- Relevant period label
- Data timestamp or status when necessary

Do not place unrelated metrics in the same card merely to fill space.

## Holdings tables

Holdings tables are a core product surface, not a secondary visual element.

They should:

- Keep asset name, symbol, exchange, and asset type easy to scan.
- Show native quoted price with its currency code.
- Show converted market value and gain or loss in USD.
- Right-align quantity, price, value, percentage, and allocation columns.
- Use tabular numerals.
- Provide clear and accessible sorting indicators.
- Preserve readable column spacing.
- Use a sticky header when the table is long.
- Keep row actions discoverable by keyboard and touch, not only on hover.
- Avoid horizontal compression that makes values ambiguous.
- Use prioritized columns, horizontal scrolling, or expandable rows on small screens.
- Provide loading, empty, error, stale, and partial-data states.

Recommended desktop columns:

```text
Asset | Quantity | Native price | USD market value | USD gain/loss | Allocation
```

Do not repeat the currency symbol without a code when multiple currencies appear in the same table.

## Charts

Charts must be accurate, interpretable, and accessible before they are visually impressive.

### Performance charts

- Display portfolio value in USD.
- Clearly label the selected range: 1D, 1W, 1M, 3M, 1Y, YTD, or ALL as supported.
- Tooltip content should include date or timestamp, USD value, and relevant change.
- State whether the series represents portfolio value, cumulative return, or another metric.
- Do not place portfolio value and percentage return on an unlabeled shared axis.
- When comparing with a benchmark, normalize both series consistently and label the benchmark.
- Percentage-return charts should visibly include the zero reference line.
- Avoid deceptive domains that exaggerate small changes; make the scale interpretable.
- Do not fill missing observations with zero.
- Show gaps or documented interpolation honestly.
- Provide a text summary of the key result outside the chart.

### Allocation charts

- Calculate allocation from USD-converted market values.
- Ensure percentages total approximately 100%, allowing only for display rounding.
- Do not use 3D charts.
- Avoid too many indistinguishable segments; group small categories into `Other` when appropriate.
- Use labels, legends, or tooltips in addition to color.
- Keep the same category color mapping across related views.

### Chart accessibility

- Make chart insights understandable without color alone.
- Provide accessible names and summaries.
- Ensure tooltips can be reached without relying exclusively on pointer hover when the chart library supports it.
- Maintain sufficient contrast for lines, text, grid lines, and selected states.

## Data states and trust

Every data-driven component must support the following states:

- Loading
- Empty
- Error
- Stale
- Partial
- Success

### Loading

Use stable skeletons or progress indicators that preserve layout. Do not show fake financial values while loading.

### Empty

Explain what is missing and give the user a relevant action.

```text
No holdings yet
Add your first transaction to start tracking portfolio value and performance.
```

### Error

Explain what failed, what remains available, and what the user can do.

```text
Market prices are temporarily unavailable
Your saved holdings are still available. Retry to refresh current prices.
```

### Stale or delayed

Make delayed data visible without overwhelming the page.

```text
Prices delayed by 15 minutes
Market data updated Jul 28, 2026, 3:45 PM EDT
FX rates updated Jul 28, 2026, 3:40 PM EDT
```

### Partial

Do not hide failed symbols inside a successful page state.

```text
Partial USD portfolio value
2 holdings could not be converted because FX data is unavailable.
```

## Forms and actions

Financial actions should feel deliberate and reversible where possible.

- Use explicit labels such as `Add transaction`, `Save changes`, and `Delete transaction`.
- Validate symbol, quantity, price, currency, and date inputs clearly.
- Put validation next to the affected field.
- Preserve entered data after recoverable errors.
- Confirm destructive actions and state their impact.
- Disable submission only when necessary and explain why.
- Prevent duplicate submission.
- Reflect the same action name in the button, confirmation, success message, and activity log.

## Writing and interface copy

Words exist to make the interface easier to understand and use.

- Write from the user's perspective, not the implementation's perspective.
- Use plain, specific language and active voice.
- Prefer `Refresh prices` over `Trigger market-data synchronization`.
- Use sentence case.
- Avoid hype, filler, apologies in error states, and vague messages.
- Let a label label, an example demonstrate, and a status explain state.
- Keep financial terminology accurate and consistent.

Do not call total value `profit`, and do not call unrealized gain `cash earned`.

## Motion

Use motion only when it clarifies state, hierarchy, or interaction.

Appropriate uses include:

- A subtle chart reveal after valid data loads
- A restrained transition when the time range changes
- Hover and focus feedback
- Expand and collapse transitions
- A brief highlight when a value refreshes

Avoid:

- Continuous number animation
- Decorative page-load sequences
- Ambient motion behind financial data
- Repeated chart drawing
- Large hover movement on cards
- Motion that implies prices are live when they are delayed

Respect `prefers-reduced-motion`.

## Accessibility

Build to a production-quality accessibility floor:

- Semantic headings and landmarks
- Visible keyboard focus
- Complete keyboard navigation
- Sufficient text and non-text contrast
- Touch targets large enough for mobile use
- Labels for icon-only controls
- Accessible names for charts and status icons
- Form errors connected to fields
- No information conveyed by color alone
- Reduced-motion support
- Logical reading and tab order

Status messages for refreshed, failed, or partially loaded data should be available to assistive technology without causing excessive announcements.

## Responsive behavior

Design for desktop, tablet, and mobile from the beginning.

- Preserve the priority of total value, period change, and data status.
- Do not shrink dense desktop tables until they become unreadable.
- Collapse secondary metrics before primary financial information.
- Use expandable holding rows or a focused holding-details view on small screens.
- Keep chart controls reachable and labels legible.
- Avoid relying on hover interactions.
- Test long asset names, large balances, negative values, and narrow widths.

## Design and build process

Work in two disciplined passes.

### Pass 1: plan

Create a compact design plan based on the project brief:

- **Subject:** Portfolio Manager for users with multi-market holdings
- **Audience:** People who need a clear, trustworthy overview of investments
- **Page job:** State the one decision or understanding the page must support
- **Color:** Define 4–6 named design tokens with hex values
- **Type:** Define body and data roles; add a display role only when justified
- **Layout:** Describe the hierarchy and provide a compact ASCII wireframe when useful
- **Signature:** Optional; identify one restrained, data-relevant distinctive element
- **States:** Identify loading, empty, error, stale, and partial behavior

### Pass 2: critique and build

Before coding, review the plan:

- Does it look like a generic AI dashboard?
- Does any distinctive choice reduce trust or readability?
- Is USD usage consistent?
- Are native prices clearly distinguished from USD-converted values?
- Can users identify delayed, stale, missing, or estimated data?
- Can the layout handle large and negative numbers?
- Is the mobile hierarchy still useful?

Revise weak choices, then implement the approved plan consistently.

Critique the built result again using screenshots when the environment supports them. Remove decoration that does not improve understanding.

## Implementation rules

- Follow the existing React, TypeScript, routing, styling, and folder structure.
- Reuse existing components and design tokens before creating new ones.
- Keep API access and financial calculations outside presentational components.
- Use typed props and typed API models.
- Centralize money, percentage, date, and quantity formatting.
- Centralize currency conversion logic; do not recalculate FX independently in UI components.
- Treat backend-calculated portfolio totals as authoritative when the API contract defines them.
- Do not install new fonts, icon packs, chart libraries, or UI frameworks unless existing dependencies cannot meet the requirement.
- Do not replace a working project architecture solely to match a visual preference.
- Keep CSS selector specificity predictable and avoid rules that unintentionally cancel spacing or typography.
- Preserve testability with deterministic components and explicit states.

## Required test cases

Review the interface with at least these cases:

- USD-only portfolio
- Portfolio containing USD, CNY, and HKD holdings
- Missing FX rate for one holding
- Missing or delayed market price
- Empty portfolio
- One holding and many holdings
- Large portfolio value
- Negative daily and total returns
- Zero gain or loss
- Fractional quantity
- Long asset name and symbol
- Narrow mobile viewport
- Keyboard-only navigation
- Reduced-motion preference

## Definition of done

A Portfolio Manager frontend task is not complete until:

- All totals and performance values use USD.
- Native security prices retain and label their market currency.
- Mixed currencies are never directly summed.
- FX and market-data freshness are visible when relevant.
- Missing values are not displayed as zero.
- Loading, empty, error, stale, and partial states are handled.
- Numeric values are aligned and consistently formatted.
- Charts are labeled, non-deceptive, and understandable without color alone.
- Tables remain usable on desktop and mobile.
- Keyboard focus and reduced-motion behavior work.
- The visual design feels intentional without competing with the data.
- Existing project conventions and dependencies are respected.


