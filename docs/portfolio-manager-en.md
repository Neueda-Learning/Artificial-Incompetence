# Portfolio Manager Training Project (English Translation)

> See [`portfolio_manager.md`](../portfolio_manager.md) for the original project description.
> For ease of team execution, this document consolidates a small amount of repeated content from the original,
> without altering the core requirements, priorities, or scope.

## 1. Project Overview

The team is to design and develop a financial portfolio management application.

A portfolio may contain one or more of the following asset types:

- Stocks
- Bonds
- Cash
- Other assets to be confirmed later

The primary task of the project is to build an application capable of saving, querying, and displaying the contents of a portfolio.

## 2. Technical Objectives

The main goal of the first training phase is to develop a portfolio management REST API. The API must be able to save and read records that describe the contents of a financial portfolio.

Once the core functionality is complete, the instructor may provide further enhancement requirements. Enhancement requirements will be more open-ended, and the team may design them drawing on members' skills and experience.

The project will continue to be developed during the frontend training phase. The frontend may use technologies learned during training; if the team wishes to adopt other frameworks or technologies, approval from the instructor is required first.

## 3. Frontend Feature Priorities

The frontend should support users in the following order of priority:

1. Browse portfolios.
2. View portfolio performance, preferably using charts.
3. Add assets to a portfolio.
4. Remove assets from a portfolio.

The instructor will act as the customer and provide detailed requirements. The team may schedule requirements-gathering meetings as needed.

## 4. Scope and Basic Assumptions

- No authentication is required initially.
- By default, there is only one user; multiple users do not need to be managed.
- Persistent storage should use the database technology learned during training.
- Git should be used appropriately, with feature branches and Pull Requests preferred.
- REST API usage documentation should be provided; if Swagger is covered in training, Swagger/OpenAPI may be used.
- The team should remain agile and avoid designing an overly complex data model at the outset.

## 5. Minimum Viable Product (MVP)

The first version should start with a very simple data model. The minimum fields suggested in the original document include:

```text
id
stockTicker
volume
```

In the context of the current project design, this can be expressed as:

```text
id
assetType
symbol
quantity
```

The first version only needs to prove that the system can:

- Add an asset.
- Query assets in the portfolio.
- Delete an asset.
- Persist data to a database.

Once the system is working, fields such as purchase price, current price, purchase date, and yield can be added incrementally.

## 6. Technical Startup Checklist

1. Create the project structure.
2. Create the Git repository.
3. Commit and push the project skeleton.
4. Ensure all team members have access to the repository.
5. Decide on the minimum fields for the first-version data model.
6. If any of these fundamental steps are blocked, seek help from the instructor as soon as possible.

## 7. Project Management Startup Checklist

1. The team collectively decides on a way of working, for example:
   - Two members handle the backend, one handles UI design;
   - Or all members work together to complete a minimal backend first.
2. Establish a task list, preferably using Trello or a similar kanban board.
3. Some members may design a more complete application while others first build a small, runnable example.
4. Prioritize the tasks needed for a minimal implementation.
5. Record questions that need to be confirmed with the instructor.
6. The team collectively decides on the initial data model, but must remain agile.

The original document specifically cautions: one of the most common problems teams face is creating an overly complex data model right from the start.

## 8. Tips for Success

- Start small — first get a simple object saving and reading successfully.
- Try pair programming.
- Maintain team communication and a positive atmosphere.
- Schedule regular team sync meetings.
- Value quality over the sheer number of features.

## 9. Team Collaboration Requirements

- The team should self-organize and work closely together.
- Potential blocking issues should be reported to the instructor promptly.
- Use a task management system to track tasks and progress.
- All source code should be managed with Git.
- Separate repositories may be set up for the frontend and backend, but all repositories must be easily accessible to team members and the instructor.
- Repositories may be public, or explicitly shared with the instructor and all team members.
- The team should conduct regular progress syncs.

## 10. Financial Market Data

The project may obtain financial data from Yahoo Finance.

Java projects may explore Yahoo Finance-related Java libraries; Python projects may use `yfinance` or read Yahoo Finance historical data directly.

The original document also provides a sample REST API that caches market data:

```text
https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker=TSLA
```

The tickers supported by the sample service by default include:

```text
C
AMZN
TSLA
FB
AAPL
```

External market data is used to obtain stock prices and historical performance; user holding data should still be stored in the project's own database.

## 11. AI Extension Directions

Once the core functionality is complete, applications of AI in portfolio management may be explored:

- Proposing rebalancing recommendations based on risk tolerance and market conditions.
- Recommending buy, hold, or sell actions.
- Predicting portfolio performance based on historical data.
- Estimating risk and potential returns.
- Supporting natural-language queries, such as "show my tech stocks."
- Performing sentiment analysis on financial news.
- Automatically generating portfolio performance summaries.
- Detecting anomalous transactions.

It is recommended to progress by difficulty level:

### Beginner

- Rule-based recommendations.
- Trend identification using basic statistical methods.
- Threshold-based alert generation.

### Intermediate

- Regression, classification, or clustering using scikit-learn.
- Training models with Yahoo historical data.
- Comparing predicted results against actual results.

### Advanced

- Integrating large language model APIs.
- Time-series forecasting using LSTM or Prophet.
- Exploring reinforcement learning trading strategies.

## 12. Quantum Computing Extension Directions

Quantum computing may be explored for:

- Portfolio allocation optimization.
- Constrained optimization of risk and return.
- Monte Carlo simulation.
- Risk metrics such as Value at Risk.
- Pattern recognition in high-dimensional financial data.

Simulators may be used instead of real quantum hardware, for example:

- IBM Qiskit
- D-Wave Ocean SDK
- Amazon Braket
- Google Cirq

A viable proof of concept is:

1. Select 4 to 5 assets.
2. Prepare expected return and covariance data.
3. Use QAOA to find asset combinations.
4. Compute the same problem using classical optimization algorithms.
5. Compare results, speed, and limitations.

Both AI and quantum features are stretch goals and should not affect the delivery of the core REST API and frontend functionality.

## 13. Optional Advanced APIs

If the team implements AI or quantum features, the following may be considered:

```http
GET  /portfolio/predictions
GET  /portfolio/optimize
POST /portfolio/query
GET  /portfolio/quantum-optimize
```

Advanced features should:

- Be clearly marked as experimental.
- Display prediction confidence levels.
- Explain the algorithm, data sources, and limitations.
- Avoid presenting experimental results as formal investment advice.

## 14. Project Presentation

The final project will be presented to the instructor, managers, or other relevant stakeholders. Presentations are typically 15 to 30 minutes, with the exact duration determined by the instructor.

Recommended presentation order:

1. Introduce the team.
2. Introduce the project background and task.
3. Explain team division of work and development methodology.
4. Introduce the technologies and tools used.
5. Present the data model.
6. Present the system architecture.
7. Conduct a live demo.
8. Explain the technical and collaboration challenges encountered.
9. Summarize lessons learned and areas for improvement.
10. Explain what would be completed if more time were available.
11. Open for Q&A.

All members are required to speak during the final presentation.

## 15. Success Criteria

Core project success criteria:

- The REST API can save, query, and delete portfolio assets.
- Data can be persisted.
- Users can browse and manage portfolios through the frontend.
- Clear documentation is provided for the API and how to run the project.
- The team uses Git, branches, and Pull Requests appropriately.
- The project can be demonstrated stably.

Stretch project success criteria:

- Can explain how AI or quantum computing may be applied to portfolio management.
- Document the research process, issues, and takeaways.
- Create a demonstrable proof of concept.
- Clearly state the limitations of experimental results and the work required for productionization.
