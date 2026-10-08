\# Money CLI



A native Android personal finance application with a terminal-style interface for managing personal transactions locally.



\## Overview



Money CLI is a simple personal finance application built with Kotlin for Android.



The application uses a terminal/CLI-style interface while storing transaction data locally using SQLite.



Money CLI V1 focuses on core financial management features without requiring an internet connection, backend, or user account.



\## Features



\- Add income

\- Add expenses

\- Cash wallet

\- BCA wallet

\- Transfer between wallets

\- View total balance

\- View balance per wallet

\- View transaction history

\- View financial statistics

\- Edit transactions

\- Delete transactions

\- Reset all transactions

\- Automatic transaction date and time

\- Transaction validation

\- Prevent negative wallet balances

\- Local SQLite database

\- Automated tests for core financial logic



\## Commands



```text

balance

transactions

stats



income add -n <amount> -d "<description>"

expense add -n <amount> -d "<description>"

transfer -n <amount>


# Money CLI

A native Android personal finance application with a terminal-style interface for managing personal transactions locally.

## Overview

Money CLI is a simple personal finance application built with Kotlin for Android.

The application uses a terminal/CLI-style interface while storing transaction data locally using SQLite.

Money CLI V1 focuses on core financial management features without requiring an internet connection, backend, or user account.

## Features

- Add income
- Add expenses
- Cash wallet
- BCA wallet
- Transfer between wallets
- View total balance
- View balance per wallet
- View transaction history
- View financial statistics
- Edit transactions
- Delete transactions
- Reset all transactions
- Automatic transaction date and time
- Transaction validation
- Prevent negative wallet balances
- Local SQLite database
- Automated tests for core financial logic
- Dual command format support

## Commands

Money CLI supports both the original command format and the shorter command format.

Both command formats perform the same operations.

### Balance

Original format:

```text
balance
transaction edit -id <id> -n <amount>

transaction delete -id <id>

transaction reset



help

clear

cancel

