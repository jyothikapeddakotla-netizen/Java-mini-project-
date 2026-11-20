import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Records that a member has borrowed a book. */
public class Loan {
    private final int bookId;
    private final int memberId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;

    public Loan(int bookId, int memberId, LocalDate issueDate, LocalDate dueDate) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
    }

    public int getBookId() { return bookId; }
    public int getMemberId() { return memberId; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }

    /** Number of days past the due date (0 if returned on time or early). */
    public long daysOverdue(LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, today));
    }

    public boolean isOverdue(LocalDate today) {
        return daysOverdue(today) > 0;
    }
}
