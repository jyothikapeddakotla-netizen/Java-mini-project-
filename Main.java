import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/** Console menu for the Library Management System. Run with: java Main */
public class Main {
    private static final Path DATA_FILE = Paths.get("library_data.txt");
    private static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        Library library;
        try {
            boolean firstRun = !DATA_FILE.toFile().exists();
            library = Library.load(DATA_FILE);
            if (firstRun) {
                addSampleData(library);
                library.save(DATA_FILE);
                System.out.println("First run: added sample books and members so you can try the menu.");
            }
        } catch (LibraryException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        System.out.println("==============================");
        System.out.println("  LIBRARY MANAGEMENT SYSTEM");
        System.out.println("==============================");

        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Choose an option: ");
            try {
                switch (choice) {
                    case "1": addBook(library); break;
                    case "2": removeBook(library); break;
                    case "3": showBooks(library.allBooks(), library); break;
                    case "4": searchBooks(library); break;
                    case "5": addMember(library); break;
                    case "6": showMembers(library); break;
                    case "7": issueBook(library); break;
                    case "8": returnBook(library); break;
                    case "9": showMemberLoans(library); break;
                    case "10": showOverdue(library); break;
                    case "0":
                        running = false;
                        System.out.println("Data saved. Goodbye!");
                        break;
                    default:
                        System.out.println("Please choose a number from the menu.");
                }
                library.save(DATA_FILE);
            } catch (LibraryException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n--- Books ---");
        System.out.println(" 1. Add book         2. Remove book");
        System.out.println(" 3. View all books   4. Search books");
        System.out.println("--- Members ---");
        System.out.println(" 5. Register member  6. View members");
        System.out.println("--- Borrowing ---");
        System.out.println(" 7. Issue book       8. Return book");
        System.out.println(" 9. Member's books  10. Overdue books");
        System.out.println(" 0. Exit");
    }

    // ------------------------------------------------------------ actions ----
    private static void addBook(Library library) throws LibraryException {
        Book book = library.addBook(prompt("Title: "), prompt("Author: "));
        System.out.println("Added: " + book);
    }

    private static void removeBook(Library library) throws LibraryException {
        int id = readInt("Book ID to remove: ");
        Book book = library.getBook(id);
        library.removeBook(id);
        System.out.println("Removed: " + book);
    }

    private static void searchBooks(Library library) throws LibraryException {
        List<Book> found = library.searchBooks(prompt("Search title or author: "));
        if (found.isEmpty()) {
            System.out.println("No books match your search.");
        } else {
            showBooks(found, library);
        }
    }

    private static void addMember(Library library) throws LibraryException {
        Member member = library.addMember(prompt("Member name: "));
        System.out.println("Registered: " + member);
    }

    private static void issueBook(Library library) throws LibraryException {
        int bookId = readInt("Book ID: ");
        int memberId = readInt("Member ID: ");
        Loan loan = library.issueBook(bookId, memberId, LocalDate.now());
        System.out.println(library.getBook(bookId).getTitle() + " issued to "
                + library.getMember(memberId).getName() + ". Due on " + loan.getDueDate() + ".");
    }

    private static void returnBook(Library library) throws LibraryException {
        int bookId = readInt("Book ID being returned: ");
        Book book = library.getBook(bookId);
        int fine = library.returnBook(bookId, LocalDate.now());
        System.out.println("\"" + book.getTitle() + "\" returned.");
        if (fine > 0) {
            System.out.println("Late fine to collect: Rs. " + fine);
        } else {
            System.out.println("Returned on time. No fine.");
        }
    }

    private static void showMemberLoans(Library library) throws LibraryException {
        Member member = library.getMember(readInt("Member ID: "));
        List<Loan> loans = library.loansOfMember(member.getId());
        if (loans.isEmpty()) {
            System.out.println(member.getName() + " has no borrowed books.");
            return;
        }
        System.out.println(member.getName() + " has borrowed:");
        for (Loan loan : loans) {
            System.out.printf("  %-30s due %s%s%n", library.getBook(loan.getBookId()).getTitle(),
                    loan.getDueDate(), loan.isOverdue(LocalDate.now()) ? "  (OVERDUE)" : "");
        }
    }

    private static void showOverdue(Library library) throws LibraryException {
        List<Loan> overdue = library.overdueLoans(LocalDate.now());
        if (overdue.isEmpty()) {
            System.out.println("No overdue books.");
            return;
        }
        System.out.println("Overdue books:");
        for (Loan loan : overdue) {
            long days = loan.daysOverdue(LocalDate.now());
            System.out.printf("  %-30s %-15s %d day(s) late, fine Rs. %d%n",
                    library.getBook(loan.getBookId()).getTitle(),
                    library.getMember(loan.getMemberId()).getName(),
                    days, days * Library.FINE_PER_DAY);
        }
    }

    // ------------------------------------------------------------ display ----
    private static void showBooks(List<Book> books, Library library) {
        if (books.isEmpty()) {
            System.out.println("The library has no books yet.");
            return;
        }
        System.out.printf("%n%-4s %-30s %-22s %s%n", "ID", "Title", "Author", "Status");
        System.out.println("---------------------------------------------------------------");
        for (Book b : books) {
            System.out.printf("%-4d %-30s %-22s %s%n", b.getId(), b.getTitle(), b.getAuthor(),
                    library.isAvailable(b.getId()) ? "Available" : "Issued");
        }
    }

    private static void showMembers(Library library) {
        if (library.allMembers().isEmpty()) {
            System.out.println("No members registered yet.");
            return;
        }
        for (Member m : library.allMembers()) {
            System.out.println("  " + m);
        }
    }

    // ------------------------------------------------------------ input ----
    private static String prompt(String message) {
        System.out.print(message);
        return in.hasNextLine() ? in.nextLine().trim() : "0"; // end of input behaves like Exit
    }

    private static int readInt(String message) {
        while (true) {
            String text = prompt(message);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static void addSampleData(Library library) throws LibraryException {
        library.addBook("Wings of Fire", "A. P. J. Abdul Kalam");
        library.addBook("The Alchemist", "Paulo Coelho");
        library.addBook("Clean Code", "Robert C. Martin");
        library.addBook("Head First Java", "Kathy Sierra");
        library.addBook("Introduction to Algorithms", "Thomas H. Cormen");
        library.addMember("Ravi Kumar");
        library.addMember("Sita Devi");
    }
}
