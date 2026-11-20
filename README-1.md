# Library Management System (Java)

A console application for managing a small library: books, members, borrowing, returns and late fines. Written in plain Java with no external libraries.

## Features

- Add, remove, view and search books (search works on title and author, ignores case)
- Register and list members
- Issue a book (due in 14 days) and return a book
- Late fine of Rs. 2 per day, calculated automatically on return
- Each member can borrow at most 3 books at a time
- View the books a member has borrowed and a list of overdue books
- Data is saved automatically to `library_data.txt`
- Clear error messages instead of crashes (wrong ID, book already issued, damaged file, and so on)
- 16 tests

## How to run

Java 8 or newer is required. Check with `java -version`.

```
javac -d out *.java
java -cp out Main
```

On the first run the program adds 5 sample books and 2 members so you can try it straight away.

## How to run the tests

```
javac -d out *.java
java -cp out LibraryTests
```

## Project structure

```
library-management-system/
├── Main.java               # console menu (input and output only)
├── Library.java            # all rules and data, plus saving and loading
├── Book.java
├── Member.java
├── Loan.java               # a borrowed book: due date, days overdue
├── LibraryException.java   # error type for broken library rules
├── LibraryTests.java       # tests
├── README.md
└── SUBMISSION_NOTES.md
```

## Design

| Class | Role |
|-------|------|
| `Book`, `Member` | Simple data classes with private fields and getters |
| `Loan` | Links a book to a member, holds the due date, works out days overdue |
| `Library` | Business rules: issue, return, fines, search, borrowing limit, file saving |
| `LibraryException` | Checked exception, so every rule violation must be handled |
| `Main` | Menu only. It never contains library rules |

`Library` does not print or read input, so it can be tested without the menu. Dates are passed in (`issueBook(bookId, memberId, today)`), which lets the tests check fines for any date without waiting for real days to pass.

## Rules

| Rule | Value |
|------|-------|
| Loan period | 14 days |
| Fine | Rs. 2 per day after the due date (none on the due date itself) |
| Borrowing limit | 3 books per member |
| Removing a book | Not allowed while it is issued |

## Data file

`library_data.txt` is a plain text file, one record per line (`B` book, `M` member, `L` loan, `C` ID counters). It is created on first run and is listed in `.gitignore`, so it is not uploaded to GitHub.
