import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Ioana on 4/18/2016.
 */
public class Session implements Comparable<Session> {
    private String session_id;
    private String start_time;
    private String end_time;
    private int duration;

    private String user;

    public Session(String user, String start, String end, int duration) {
        this.user = user;
        this.start_time = start;
        this.end_time = end;
        this.duration = duration;
    }

    public Session(String session_id, String user, String start, String end, int duration) {
        this.session_id = session_id;
        this.user = user;
        this.start_time = start;
        this.end_time = end;
        this.duration = duration;
    }

    public int getDuration() { return this.duration; }

    public String getSessionId() { return this.session_id; }

    public boolean includes(String user_id, String start, String end) {

        if(user.compareTo(user_id) != 0)
            return false;

        if(start_time.compareTo(start) <= 0 && end_time.compareTo(end) >= 0)
            return true;

        return false;
    }

    public boolean includesStart(String user_id, String start) {

        if(user.compareTo(user_id) != 0)
            return false;

        if(start_time.compareTo(start) <= 0 && end_time.compareTo(start) > 0)
            return true;

        return false;
    }

    private Date getDateFromString(String dateString) {
        //input date: "2014-11-11 12:00:00"
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return format.parse(dateString);
        }
        catch (ParseException e) {
            System.out.println("Invalid date");
            return new Date();
        }
    }

    public int compareTo(Session next) {
        return this.start_time.compareTo(next.start_time);
    }

    public String getStartTime() {
        return this.start_time;
    }

    public String getEndTime() {
        return this.end_time;
    }

    public Date getStartDate() {
        return getDateFromString(start_time);
    }

    public Date getEndDate() {
        return getDateFromString(end_time);
    }

}

class ForumSession extends Session {
    public ForumSession(String user, String start, String end, int duration) {
        super(user, start, end, duration);
    }
}

class QuizSession extends Session {

    public QuizSession(String user, String start, String end, int duration) {
        super(user, start, end, duration);
    }

    public QuizSession(String session_id, String user, String start, String end, int duration) {
        super(session_id, user, start, end, duration);
    }
}

class VideoSession extends Session {
    private String video_id;
    private int video_length;

    public VideoSession(String user, String video, String start, String end, int duration) {
        super(user, start, end, duration);
        this.video_id = video;
    }

    public VideoSession(String user, String video, String start, String end, int duration, int length) {
        super(user, start, end, duration);
        this.video_id = video;
        this.video_length = length;
    }

    public String getVideoId() {
        return this.video_id;
    }

    public int getVideoLength() { return this.video_length; }
}