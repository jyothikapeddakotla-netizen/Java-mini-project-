import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * All the library rules and data. This class never prints or reads input,
 * so it can be tested without the menu.
 */
public class Library {
    public static final int MAX_BOOKS_PER_MEMBER = 3;
    public static final int LOAN_DAYS = 14;
    public static final int FINE_PER_DAY = 2; // rupees

    private final TreeMap<Integer, Book> books = new TreeMap<>();
    private final TreeMap<Integer, Member> members = new TreeMap<>();
    private final List<Loan> loans = new ArrayList<>();
    private int nextBookId = 1;
    private int nextMemberId = 1;

    // ------------------------------------------------------------ books ----
    public Book addBook(String title, String author) throws LibraryException {
        Book book = new Book(nextBookId, require(title, "Title"), require(author, "Author"));
        nextBookId++;
        books.put(book.getId(), book);
        return book;
    }

    public void removeBook(int bookId) throws LibraryException {
        Book book = getBook(bookId);
        if (loanForBook(bookId) != null) {
            throw new LibraryException("\"" + book.getTitle() + "\" is currently issued and cannot be removed.");
        }
        books.remove(bookId);
    }

    public Book getBook(int bookId) throws LibraryException {
        Book book = books.get(bookId);
        if (book == null) {
            throw new LibraryException("No book found with ID " + bookId + ".");
        }
        return book;
    }

    public List<Book> allBooks() {
        return Collections.unmodifiableList(new ArrayList<>(books.values()));
    }

    /** Case-insensitive search in the title and the author. */
    public List<Book> searchBooks(String keyword) throws LibraryException {
        String word = require(keyword, "Search word").toLowerCase();
        List<Book> found = new ArrayList<>();
        for (Book book : books.values()) {
            if (book.getTitle().toLowerCase().contains(word) || book.getAuthor().toLowerCase().contains(word)) {
                found.add(book);
            }
        }
        return found;
    }

    public boolean isAvailable(int bookId) {
        return loanForBook(bookId) == null;
    }

    // ---------------------------------------------------------- members ----
    public Member addMember(String name) throws LibraryException {
        Member member = new Member(nextMemberId, require(name, "Name"));
        nextMemberId++;
        members.put(member.getId(), member);
        return member;
    }

    public Member getMember(int memberId) throws LibraryException {
        Member member = members.get(memberId);
        if (member == null) {
            throw new LibraryException("No member found with ID " + memberId + ".");
        }
        return member;
    }

    public List<Member> allMembers() {
        return Collections.unmodifiableList(new ArrayList<>(members.values()));
    }

    // ------------------------------------------------------------ loans ----
    public Loan issueBook(int bookId, int memberId, LocalDate today) throws LibraryException {
        Book book = getBook(bookId);
        Member member = getMember(memberId);
        if (loanForBook(bookId) != null) {
            throw new LibraryException("\"" + book.getTitle() + "\" is already issued to someone else.");
        }
        if (loansOfMember(memberId).size() >= MAX_BOOKS_PER_MEMBER) {
            throw new LibraryException(member.getName() + " already has " + MAX_BOOKS_PER_MEMBER
                    + " books. Return one before borrowing another.");
        }
        Loan loan = new Loan(bookId, memberId, today, today.plusDays(LOAN_DAYS));
        loans.add(loan);
        return loan;
    }

    /** Returns the book and gives back the fine in rupees (0 if on time). */
    public int returnBook(int bookId, LocalDate today) throws LibraryException {
        Book book = getBook(bookId);
        Loan loan = loanForBook(bookId);
        if (loan == null) {
            throw new LibraryException("\"" + book.getTitle() + "\" is not issued to anyone.");
        }
        int fine = (int) (loan.daysOverdue(today) * FINE_PER_DAY);
        loans.remove(loan);
        return fine;
    }

    public List<Loan> loansOfMember(int memberId) {
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.getMemberId() == memberId) {
                result.add(loan);
            }
        }
        return result;
    }

    public List<Loan> overdueLoans(LocalDate today) {
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.isOverdue(today)) {
                result.add(loan);
            }
        }
        return result;
    }

    private Loan loanForBook(int bookId) {
        for (Loan loan : loans) {
            if (loan.getBookId() == bookId) {
                return loan;
            }
        }
        return null;
    }

    // ------------------------------------------------------ persistence ----
    // File format, one record per line, fields separated by "|":
    //   C|nextBookId|nextMemberId
    //   B|id|title|author
    //   M|id|name
    //   L|bookId|memberId|issueDate|dueDate
    public void save(Path file) throws LibraryException {
        List<String> lines = new ArrayList<>();
        lines.add("C|" + nextBookId + "|" + nextMemberId);
        for (Book b : books.values()) {
            lines.add("B|" + b.getId() + "|" + b.getTitle() + "|" + b.getAuthor());
        }
        for (Member m : members.values()) {
            lines.add("M|" + m.getId() + "|" + m.getName());
        }
        for (Loan l : loans) {
            lines.add("L|" + l.getBookId() + "|" + l.getMemberId() + "|" + l.getIssueDate() + "|" + l.getDueDate());
        }
        try {
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); // swap only after a full write
        } catch (IOException e) {
            throw new LibraryException("Could not save data: " + e.getMessage(), e);
        }
    }

    /** Loads a saved library. Returns an empty library if the file does not exist yet. */
    public static Library load(Path file) throws LibraryException {
        Library library = new Library();
        if (!Files.exists(file)) {
            return library;
        }
        try {
            int savedBookCounter = 1;
            int savedMemberCounter = 1;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] p = line.split("\\|", -1);
                switch (p[0]) {
                    case "C":
                        savedBookCounter = Integer.parseInt(p[1]);
                        savedMemberCounter = Integer.parseInt(p[2]);
                        break;
                    case "B":
                        Book book = new Book(Integer.parseInt(p[1]), p[2], p[3]);
                        library.books.put(book.getId(), book);
                        break;
                    case "M":
                        Member member = new Member(Integer.parseInt(p[1]), p[2]);
                        library.members.put(member.getId(), member);
                        break;
                    case "L":
                        library.loans.add(new Loan(Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                                LocalDate.parse(p[3]), LocalDate.parse(p[4])));
                        break;
                    default:
                        throw new LibraryException("Unknown record type: " + p[0]);
                }
            }
            for (Loan loan : library.loans) {
                if (!library.books.containsKey(loan.getBookId()) || !library.members.containsKey(loan.getMemberId())) {
                    throw new LibraryException("A loan refers to a book or member that does not exist.");
                }
            }
            // Never reuse an ID, even if the data file was edited by hand.
            library.nextBookId = Math.max(savedBookCounter, library.books.isEmpty() ? 1 : maxKey(library.books) + 1);
            library.nextMemberId = Math.max(savedMemberCounter, library.members.isEmpty() ? 1 : maxKey(library.members) + 1);
            return library;
        } catch (IOException e) {
            throw new LibraryException("Could not read " + file + ": " + e.getMessage(), e);
        } catch (NumberFormatException | DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            throw new LibraryException("The data file " + file + " is damaged.", e);
        }
    }

    private static int maxKey(TreeMap<Integer, ?> map) {
        return map.lastKey();
    }

    // ---------------------------------------------------------- helpers ----
    /** Trims the text, rejects blanks, and removes "|" so it cannot break the data file. */
    private static String require(String text, String fieldName) throws LibraryException {
        String clean = text == null ? "" : text.trim().replace("|", "/");
        if (clean.isEmpty()) {
            throw new LibraryException(fieldName + " cannot be empty.");
        }
        return clean;
    }
}
