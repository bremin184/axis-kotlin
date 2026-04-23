# Axis Budget — M-Pesa Smart Budgeting Application

## Overview

**Axis Budget** is a native Android financial intelligence application designed specifically for M-Pesa users. The application passively reads transaction data from SMS messages, extracts structured financial information, and provides real-time budgeting, goal tracking, and financial insights without directly handling or moving user funds.

The system is built as an **offline-first, privacy-focused solution**, leveraging local processing to ensure user data remains secure while delivering meaningful financial analytics.

---

## Core Philosophy

- **No financial custody**: The app does not store, transfer, or interact with user funds.
- **Data-driven insights**: All intelligence is derived from transaction SMS parsing.
- **Local-first processing**: Sensitive data is processed and stored on-device.
- **User-controlled customization**: Budgets, categories, and goals are fully configurable.

---

## Key Features

### 1. SMS-Based Transaction Ingestion

- Reads M-Pesa SMS messages from the device inbox (Android only).
- Filters messages by sender (`MPESA`).
- Extracts structured transaction data including:
  - Amount
  - Transaction type (sent, received, paybill, buy goods, etc.)
  - Recipient / merchant
  - Phone number
  - Timestamp
  - Transaction fee
  - Account balance

---

### 2. Intelligent Parsing Engine

- Regex-based extraction for MVP implementation.
- Modular parser architecture (`MpesaParser.kt`) for future NLP/AI upgrades.
- Handles multiple M-Pesa transaction formats:
  - Send Money
  - Receive Money
  - Paybill
  - Buy Goods
  - Airtime
  - Fuliza
  - Reversals

---

### 3. Multi-Fund Financial Modeling

The application distinguishes between different financial sources within the M-Pesa ecosystem:

#### Supported Funds (Core)

| Fund Type        | Classification          | Behavior |
|-----------------|------------------------|----------|
| Personal Balance | Default                | Primary wallet |
| Pochi           | Business (Individual)  | User-labeled business wallet |
| Till            | Business (Merchant)    | Treated as business income/expense |
| M-Shwari        | Savings / Loans        | Split into savings and short-term liabilities |
| Zidii           | Investment             | Includes ticker extraction from SMS |

#### Extended (Optional Activation)

- M-Akiba → Investment
- M-Tiba / Linda Jamii / Bima → Health & Insurance
- KCB Loans → Long-term liabilities

Each fund type is:
- Independently tracked
- Categorized
- Viewable in dedicated UI sections
- Expandable with contextual education (info panels)

---

### 4. Smart Categorization Engine

- Rule-based mapping using merchant keywords.
- User-corrected categories are learned and reused.
- Supports:
  - Food
  - Transport
  - Bills
  - Investment
  - Loans
  - Insurance
  - Custom categories

---

### 5. Budget Engine

- User-defined monthly budgets per category.
- Real-time deduction based on parsed transactions.
- Threshold-based alerts:
  - 80% usage warning
  - Budget exceeded notification

---

### 6. Goal Projection Engine

- Users define financial goals (e.g., savings targets).
- System calculates:
  - Time to goal completion
  - Required savings rate
  - Feasibility based on current behavior

---

### 7. Micro-Transaction Detection

- Identifies recurring small expenses that accumulate over time.
- Highlights patterns such as:
  - Frequent airtime purchases
  - Repeated small food transactions
- Provides actionable insights to reduce leakage.

---

### 8. Financial Health Score

Composite score (0–100) based on:

- Budget adherence
- Savings rate
- Loan exposure
- Spending consistency
- Investment activity

---

### 9. Privacy & Security

- All SMS parsing occurs locally on device.
- No external transmission of raw SMS data.
- Optional encryption via SQLCipher for local database.
- Clear user consent required for SMS access.

---

## Technical Architecture

### Platform

- **Android (Native)**
- Language: **Kotlin**
- Architecture: **Modular, layered**

---

### Project Structure
---

### Core Components

#### Parser Layer
- `MpesaParser.kt`
- `RegexPatterns.kt`

#### Data Layer
- `Transaction.kt`
- `FundType.kt`
- `BudgetCategory.kt`

#### Business Logic
- `BudgetEngine.kt`
- `GoalEngine.kt`
- `HealthScore.kt`

#### Storage
- Room Database (`AppDatabase.kt`)
- DAO interfaces for transactions

---

## Development Phases

### Phase 1 — MVP
- SMS parsing (sample + manual input)
- Basic UI (dashboard + transaction list)
- Simple categorization

### Phase 2 — Core Features
- Real SMS inbox integration
- Budget engine
- Fund differentiation
- Goal projection

### Phase 3 — Intelligence Layer
- Micro-spending detection
- Financial health scoring
- Pattern recognition

### Phase 4 — Advanced
- OCR receipt ingestion
- Predictive analytics
- Behavioral insights

---

## Limitations

- SMS access is **Android-only** (not supported on iOS).
- Web version cannot access SMS directly:
  - Uses manual paste / CSV import / demo data instead.
- Regex parsing may require updates as M-Pesa formats evolve.

---

## Future Enhancements

- Machine learning-based transaction classification
- Spending prediction models
- Credit scoring from behavioral data
- Cloud sync (optional, encrypted)
- Multi-device support

---

## Use Case Relevance

This application is optimized for regions with high M-Pesa adoption, particularly:

- Kenya
- East Africa

It addresses a critical gap:

> Most users transact digitally but lack automated financial tracking tools.

---

## Conclusion

Axis Budget transforms raw SMS transaction data into structured financial intelligence. By combining local processing, intelligent categorization, and predictive analytics, it provides users with a clear understanding of their financial behavior without introducing regulatory complexity.

The architecture is designed for scalability, enabling future expansion into advanced financial analytics while maintaining a strong privacy-first foundation.
