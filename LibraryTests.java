import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Simple tests that need no extra libraries.
 * Run with:  java -cp out LibraryTests   (see README for the compile command)
 */
public class LibraryTests {
    private static int passed = 0;
    private static int failed = 0;
    private static final LocalDate DAY = LocalDate.of(2026, 1, 1);

    public static void main(String[] args) throws Exception {
        test("book IDs are sequential", () -> {
            Library lib = new Library();
            check(lib.addBook("A", "X").getId() == 1);
            check(lib.addBook("B", "Y").getId() == 2);
        });

        test("empty title or author is rejected", () -> {
            Library lib = new Library();
            expectError(() -> lib.addBook("   ", "X"));
            expectError(() -> lib.addBook("A", ""));
            expectError(() -> lib.addMember(" "));
        });

        test("the | character is cleaned so it cannot break the data file", () -> {
            Library lib = new Library();
            check(lib.addBook("A|B", "X").getTitle().equals("A/B"));
        });

        test("issue sets the due date 14 days later", () -> {
            Library lib = sample();
            Loan loan = lib.issueBook(1, 1, DAY);
            check(loan.getDueDate().equals(DAY.plusDays(14)));
            check(!lib.isAvailable(1));
        });

        test("return on time has no fine and frees the book", () -> {
            Library lib = sample();
            lib.issueBook(1, 1, DAY);
            check(lib.returnBook(1, DAY.plusDays(14)) == 0); // exactly on the due date
            check(lib.isAvailable(1));
        });

        test("late return charges Rs. 2 per day", () -> {
            Library lib = sample();
            lib.issueBook(1, 1, DAY);
            check(lib.returnBook(1, DAY.plusDays(14 + 5)) == 10);
        });

        test("cannot issue a book that is already issued", () -> {
            Library lib = sample();
            lib.issueBook(1, 1, DAY);
            expectError(() -> lib.issueBook(1, 2, DAY));
        });

        test("a member can borrow at most 3 books", () -> {
            Library lib = sample();
            lib.issueBook(1, 1, DAY);
            lib.issueBook(2, 1, DAY);
            lib.issueBook(3, 1, DAY);
            expectError(() -> lib.issueBook(4, 1, DAY));
            check(lib.loansOfMember(1).size() == 3);
        });

        test("returning a book that is not issued fails", () -> {
            Library lib = sample();
            expectError(() -> lib.returnBook(1, DAY));
        });

        test("unknown book or member IDs fail", () -> {
            Library lib = sample();
            expectError(() -> lib.issueBook(99, 1, DAY));
            expectError(() -> lib.issueBook(1, 99, DAY));
            expectError(() -> lib.getBook(99));
        });

        test("an issued book cannot be removed, an available one can", () -> {
            Library lib = sample();
            lib.issueBook(1, 1, DAY);
            expectError(() -> lib.removeBook(1));
            lib.removeBook(2);
            check(lib.allBooks().size() == 3);
        });

        test("search ignores case and checks title and author", () -> {
            Library lib = sample();
            check(lib.searchBooks("clean").size() == 1);
            check(lib.searchBooks("PAULO").size() == 1);
            check(lib.searchBooks("zzz").isEmpty());
            expectError(() -> lib.searchBooks("  "));
        });

        test("overdue list only has late loans", () -> {
            Library lib = sample();
            lib.issueBook(1, 1, DAY);
            lib.issueBook(2, 2, DAY.plusDays(10));
            List<Loan> overdue = lib.overdueLoans(DAY.plusDays(20));
            check(overdue.size() == 1 && overdue.get(0).getBookId() == 1);
        });

        test("save and load keep books, members, loans and ID counters", () -> {
            Path file = Files.createTempFile("library", ".txt");
            try {
                Library lib = sample();
                lib.issueBook(1, 1, DAY);
                lib.removeBook(4); // highest ID removed: counter must not go backwards
                lib.save(file);

                Library loaded = Library.load(file);
                check(loaded.allBooks().size() == 3);
                check(loaded.allMembers().size() == 2);
                check(!loaded.isAvailable(1));
                check(loaded.loansOfMember(1).get(0).getDueDate().equals(DAY.plusDays(14)));
                check(loaded.addBook("New", "Z").getId() == 5);
            } finally {
                Files.deleteIfExists(file);
            }
        });

        test("loading a missing file gives an empty library", () -> {
            Path missing = Files.createTempFile("library", ".txt");
            Files.delete(missing);
            check(Library.load(missing).allBooks().isEmpty());
        });

        test("loading a damaged file gives a clear error", () -> {
            Path file = Files.createTempFile("library", ".txt");
            try {
                Files.write(file, "B|abc|Title|Author".getBytes(StandardCharsets.UTF_8));
                expectError(() -> Library.load(file));
                Files.write(file, "L|1|1|2026-01-01|2026-01-15".getBytes(StandardCharsets.UTF_8));
                expectError(() -> Library.load(file)); // loan without book or member
            } finally {
                Files.deleteIfExists(file);
            }
        });

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------- helpers ----
    private static Library sample() throws LibraryException {
        Library lib = new Library();
        lib.addBook("Wings of Fire", "A. P. J. Abdul Kalam");
        lib.addBook("The Alchemist", "Paulo Coelho");
        lib.addBook("Clean Code", "Robert C. Martin");
        lib.addBook("Head First Java", "Kathy Sierra");
        lib.addMember("Ravi");
        lib.addMember("Sita");
        return lib;
    }

    private interface Body { void run() throws Exception; }

    private static void test(String name, Body body) {
        try {
            body.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL  " + name + "  -> " + t);
        }
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("check failed");
        }
    }

    private static void expectError(Body body) throws Exception {
        try {
            body.run();
        } catch (LibraryException expected) {
            return;
        }
        throw new AssertionError("expected a LibraryException but none was thrown");
    }
}
