import java.text.ParseException;

/**
 * Created by Ioana on 6/8/2016.
 */
public class Submission {
    private String user;
    private String problem;
    private String timestamp;
    private String deadline;

    public Submission(String user, String problem, String timestamp, String deadline) {
        this.user = user;
        this.problem = problem;
        this.timestamp = timestamp;
        this.deadline = deadline;
    }

    public String getProblem() { return this.problem; }

    public String getTimestamp() { return this.timestamp; }

    public String getDeadline() { return this.deadline; }

    public long getTimeliness(){
        return Utils.differenceBetweenDatesInHours(deadline, timestamp);
    }
}
