# Mock Data Folder

This folder contains demo data for local frontend preview when backend data is unavailable.

## Enable mock data mode

Run:

npm run start:mock

This starts the app with `REACT_APP_USE_MOCK_DATA=true`.

## Behavior

- Initial mock items are loaded from `portfolio-items.json` once per browser.
- Add Asset / Remove Asset updates are persisted to browser localStorage.
- Real API mode is still the default when using `npm start`.

## Reset mock data in browser

In DevTools Console:

localStorage.removeItem("portfolio-manager-mock-items-v1");
localStorage.removeItem("portfolio-manager-mock-seeded-v1");

Reload the page to reseed from `portfolio-items.json`.

## One-click cleanup after real backend is connected

Run:

npm run mock:cleanup

This removes the entire `public/mock-data` folder.
