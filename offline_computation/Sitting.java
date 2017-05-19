import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ioana on 6/17/2016.
 */

public class Sitting {

    static HashMap<String, UserForDataCuration> users;

    private static void initialize() {
        users = new HashMap<>();
    }

    public static void computeSittings(String learner_file, String dataPath) throws IOException, ParseException {
        String curatedDataPath = dataPath + "learning_activities\\";

        Utils.checkForDirectory(curatedDataPath);

        initialize();

        readUsers(users, learner_file);

        readQuizSessions(dataPath + "quiz_sessions.csv");
        cleanQuizSessions();
        writeQuizSessions(curatedDataPath + "new_quiz_sessions.csv");

        readForumSessions(dataPath + "forum_sessions.csv");
        cleanForumSessions();
        writeForumSessions(curatedDataPath + "new_forum_sessions.csv");

        readObservations(dataPath + "video_interaction.csv");
        cleanVideoSessions();
        writeVideoSessions(curatedDataPath + "new_video_sessions.csv");

        createActivitySessions();
        createArtificalSessions();
        writeActivitySessions(curatedDataPath + "activity_sessions.csv");
        writeArtificialSessions(dataPath + "sittings.csv");

    }

    //READ
    //Reads from a file with ids on the first column and creates a Hashmap of UserMetricComputation objects with the id as key
    private static void readUsers(HashMap<String, UserForDataCuration> group, String filename) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String [] nextLine;

        csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null)
            group.put(nextLine[0], new UserForDataCuration(nextLine[0]));

        csvReader.close();

        System.out.println("Users read: " + group.size());
    }

    private static void readForumSessions(String filename) throws IOException, ParseException {
        //session_id, course_user_id, ??, start_time, end_time, duration
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String[] nextLine, session_attr;
        int duration, read = 0;
        String userId, start, end;

        //nextLine = csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null) {
            userId = nextLine[1];
            start = nextLine[3];
            end = nextLine[4];
            duration = Integer.parseInt(nextLine[5]);


            if(users.containsKey(userId)) {

                users.get(userId).addForumSession(new Session(userId, start, end, duration));

                read++;
            }
        }

        csvReader.close();

        System.out.println("Forum sessions read: " + read);
    }

    private static void readQuizSessions(String filename) throws IOException, ParseException {
        //session_id, course_user_id, start_time, end_time, duration
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String[] nextLine;
        String session_id, userId, start, end;
        int duration, read = 0;

        //nextLine = csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null) {
            session_id = nextLine[0];
            userId = nextLine[1];
            start = nextLine[2];
            end = nextLine[3];
            duration = Integer.parseInt(nextLine[4]);

            if(users.containsKey(userId)) {

                users.get(userId).addQuizSession(new QuizSession(session_id, userId, start, end, duration));

                read++;
            }
        }

        csvReader.close();

        System.out.println("Quiz sessions read: " + read);
    }

    private static void readObservations(String filename) throws IOException, ParseException {
        //obs_id, course_user_id, video_id, duration, start_time, end_time
        //obs_id: DelftX/RI101x/3T2014_623509_i4x-DelftX-RI101x-video-a40b7388bcf04df992403da80db8b3ec_2014-12-12 14:30:11
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String[] nextLine;
        int duration, read = 0;
        String userId, start, end;

        //nextLine = csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null) {
            userId = nextLine[1];
            start = nextLine[12];
            end = nextLine[13];
            duration = Integer.parseInt(nextLine[3]);

            if(users.containsKey(userId)) {

                users.get(userId).addVideoSession(new Session(userId, start, end, duration));

                read++;
            }
        }

        csvReader.close();

        System.out.println("Video sessions read: " + read);
    }

    //CURATE
    private static void cleanQuizSessions() throws ParseException {
        List<Session> newQuizSessions;
        Session current;
        String newStartTime, newEndTime;

        for (Map.Entry<String, UserForDataCuration> entry : users.entrySet()) {
            List<Session> quizSessions = entry.getValue().getQuizSessions();
            newQuizSessions = new ArrayList<>();

            Collections.sort(quizSessions);

            for(int i = 0, j; i < quizSessions.size(); i = j) {
                newStartTime = quizSessions.get(i).getStartTime();
                newEndTime = quizSessions.get(i).getEndTime();

                j = i + 1;
                while (j < quizSessions.size()) {
                    current = quizSessions.get(j);

                    if (newEndTime.compareTo(current.getStartTime()) >= 0) {
                        if(newEndTime.compareTo(current.getEndTime()) < 0)
                            newEndTime = current.getEndTime();
                        j++;
                    }
                    else
                        break;
                }

                int duration = (int) getDifferenceInSeconds(newStartTime, newEndTime);

                newQuizSessions.add(new Session(entry.getKey(), newStartTime, newEndTime, duration));

            }

            entry.getValue().setQuizSessions(newQuizSessions);

        }
    }

    private static void cleanForumSessions() throws ParseException {
        List<Session> newForumSessions;
        Session current;
        String newStartTime, newEndTime;

        for (Map.Entry<String, UserForDataCuration> entry : users.entrySet()) {
            List<Session> forumSessions = entry.getValue().getForumSessions();
            newForumSessions = new ArrayList<>();

            Collections.sort(forumSessions);

            for(int i = 0, j; i < forumSessions.size(); i = j) {
                newStartTime = forumSessions.get(i).getStartTime();
                newEndTime = forumSessions.get(i).getEndTime();

                j = i + 1;
                while (j < forumSessions.size()) {
                    current = forumSessions.get(j);

                    if (newEndTime.compareTo(current.getStartTime()) >= 0) {
                        if(newEndTime.compareTo(current.getEndTime()) < 0)
                            newEndTime = current.getEndTime();
                        j++;
                    }
                    else
                        break;
                }

                int duration = (int) getDifferenceInSeconds(newStartTime, newEndTime);

                newForumSessions.add(new Session(entry.getKey(), newStartTime, newEndTime, duration));

            }

            entry.getValue().setForumSessions(newForumSessions);

        }
    }

    private static void cleanVideoSessions() throws ParseException {
        List<Session> newVideoSessions;
        Session current;
        String newStartTime, newEndTime;

        for (Map.Entry<String, UserForDataCuration> entry : users.entrySet()) {
            List<Session> videoSessions = entry.getValue().getVideoSessions();
            newVideoSessions = new ArrayList<>();

            Collections.sort(videoSessions);

            for(int i = 0, j; i < videoSessions.size(); i = j) {
                newStartTime = videoSessions.get(i).getStartTime();
                newEndTime = videoSessions.get(i).getEndTime();

                j = i + 1;
                while (j < videoSessions.size()) {
                    current = videoSessions.get(j);

                    if (newEndTime.compareTo(current.getStartTime()) >= 0) {
                        if(newEndTime.compareTo(current.getEndTime()) < 0)
                            newEndTime = current.getEndTime();
                        j++;
                    }
                    else
                        break;
                }

                int duration = (int) getDifferenceInSeconds(newStartTime, newEndTime);

                newVideoSessions.add(new Session(entry.getKey(), newStartTime, newEndTime, duration));

            }

            entry.getValue().setVideoSessions(newVideoSessions);

        }
    }

    private static void createActivitySessions() throws ParseException {
        List<Session> allActivitySessions;
        List<Session> activeSessions;
        Session current;
        String newStartTime, newEndTime;

        for (Map.Entry<String, UserForDataCuration> entry : users.entrySet()) {
            activeSessions = new ArrayList<>();
            allActivitySessions = new ArrayList<>();

            allActivitySessions.addAll(entry.getValue().getForumSessions());
            allActivitySessions.addAll(entry.getValue().getQuizSessions());
            allActivitySessions.addAll(entry.getValue().getVideoSessions());

            Collections.sort(allActivitySessions);

            for(int i = 0, j; i < allActivitySessions.size(); i = j) {
                newStartTime = allActivitySessions.get(i).getStartTime();
                newEndTime = allActivitySessions.get(i).getEndTime();

                j = i + 1;
                while (j < allActivitySessions.size()) {
                    current = allActivitySessions.get(j);

                    if (newEndTime.compareTo(current.getStartTime()) >= 0) {
                        if(newEndTime.compareTo(current.getEndTime()) < 0)
                            newEndTime = current.getEndTime();
                        j++;
                    }
                    else
                        break;
                }

                int duration = (int) getDifferenceInSeconds(newStartTime, newEndTime);

                activeSessions.add(new Session(entry.getKey(), newStartTime, newEndTime, duration));

            }

            entry.getValue().setActivitySessions(activeSessions);

        }
    }

    private static void createArtificalSessions() throws ParseException {
        List<Session> allActivitySessions;
        Session current;
        String newStartTime, newEndTime;

        for (Map.Entry<String, UserForDataCuration> entry : users.entrySet()) {
            allActivitySessions = entry.getValue().getActivitySessions();

            Collections.sort(allActivitySessions);

            for(int i = 0, j; i < allActivitySessions.size(); i = j) {
                newStartTime = allActivitySessions.get(i).getStartTime();
                newEndTime = allActivitySessions.get(i).getEndTime();

                j = i + 1;
                while (j < allActivitySessions.size()) {
                    current = allActivitySessions.get(j);

                    if (getDifferenceInSeconds(newEndTime, current.getStartTime()) > 1800)
                        break;

                    newEndTime = current.getEndTime();
                    j++;

                }

                int duration = (int) getDifferenceInSeconds(newStartTime, newEndTime);

                entry.getValue().addSession(new Session(entry.getKey(), newStartTime, newEndTime, duration));

            }

        }

    }

    //WRITE
    private static void writeQuizSessions(String filename) throws IOException {
        CSVWriter output = new CSVWriter(new FileWriter(filename), ',');
        String[] toWrite;
        List<Session> quizSessions;

        toWrite = "Session_id#User_id#Start time#End time#Duration".split("#");

        output.writeNext(toWrite);

        for (Map.Entry<String, UserForDataCuration> entry : users.entrySet()) {

            quizSessions = entry.getValue().getQuizSessions();

            for (Session s: quizSessions) {
                toWrite[0] = s.getSessionId();
                toWrite[1] = entry.getKey();
                toWrite[2] = s.getStartTime();
                toWrite[3] = s.getEndTime();
                toWrite[4] = String.valueOf(s.getDuration());

                output.writeNext(toWrite);
            }

        }
        output.close();
    }

    private static void writeForumSessions(String filename) throws IOException {
        CSVWriter output = new CSVWriter(new FileWriter(filename), ',');
        String[] toWrite;
        List<Session> forumSessions;

        toWrite = "User_id#Start time#End time#Duration".split("#");

        output.writeNext(toWrite);

        for (Map.Entry<String, UserForDataCuration> entry : users.entrySet()) {

            forumSessions = entry.getValue().getForumSessions();

            for (int i = 0; i < forumSessions.size(); i++) {
                Session s = forumSessions.get(i);

                toWrite[0] = entry.getKey();
                toWrite[1] = s.getStartTime();
                toWrite[2] = s.getEndTime();
                toWrite[3] = String.valueOf(s.getDuration());

                output.writeNext(toWrite);
            }

        }
        output.close();
    }

    private static void writeVideoSessions(String filename) throws IOException {
        CSVWriter output = new CSVWriter(new FileWriter(filename), ',');
        String[] toWrite;
        List<Session> videoSessions;

        toWrite = "User_id#Start time#End time#Duration".split("#");

        output.writeNext(toWrite);

        for (Map.Entry<String, UserForDataCuration> entry : users.entrySet()) {

            videoSessions = entry.getValue().getVideoSessions();

            for (int i = 0; i < videoSessions.size(); i++) {
                Session s = videoSessions.get(i);

                toWrite[0] = entry.getKey();
                toWrite[1] = s.getStartTime();
                toWrite[2] = s.getEndTime();
                toWrite[3] = String.valueOf(s.getDuration());

                output.writeNext(toWrite);
            }

        }
        output.close();
    }

    private static void writeActivitySessions(String filename) throws IOException {
        CSVWriter output = new CSVWriter(new FileWriter(filename), ',');
        String[] toWrite;
        List<Session> activeSessions;

        toWrite = "User_id#Start time#End time#Duration".split("#");

        output.writeNext(toWrite);

        for (Map.Entry<String, UserForDataCuration> entry : users.entrySet()) {

            activeSessions = entry.getValue().getActivitySessions();

            for (int i = 0; i < activeSessions.size(); i++) {
                Session s = activeSessions.get(i);

                toWrite[0] = entry.getKey();
                toWrite[1] = s.getStartTime();
                toWrite[2] = s.getEndTime();
                toWrite[3] = String.valueOf(s.getDuration());

                output.writeNext(toWrite);
            }

        }
        output.close();
    }

    private static void writeArtificialSessions(String filename) throws IOException {
        CSVWriter output = new CSVWriter(new FileWriter(filename), ',');
        String[] toWrite;
        List<Session> sessions;

        toWrite = "User_id#Start time#End time#Duration".split("#");

        output.writeNext(toWrite);

        for (Map.Entry<String, UserForDataCuration> entry : users.entrySet()) {

            sessions = entry.getValue().getSessions();


            for (int i = 0; i < sessions.size(); i++) {
                Session s = sessions.get(i);

                toWrite[0] = entry.getKey();
                toWrite[1] = s.getStartTime();
                toWrite[2] = s.getEndTime();
                toWrite[3] = String.valueOf(s.getDuration());

                output.writeNext(toWrite);
            }

        }
        output.close();
    }

    //UTILS
    private static long getDifferenceInSeconds(String start, String end) throws ParseException {
        Date startDate = getDateFromString(start);
        Date endDate = getDateFromString(end);

        long diff = endDate.getTime() - startDate.getTime();

        return TimeUnit.MILLISECONDS.toSeconds(diff);

    }

    private static Date getDateFromString(String dateString) throws ParseException {
        //input date: "2014-11-11 12:00:00"
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return format.parse(dateString);
        }
        catch (ParseException e) {
            return format.parse(dateString.replace('T', ' ').substring(0, dateString.length() - 2));
        }
    }


}

class UserForDataCuration {
    private String id;

    private List<Session> sessions;
    private List<Session> activitySessions;
    private List<Session> quizSessions;
    private List<Session> forumSessions;
    private List<Session> videoSessions;

    public UserForDataCuration (String id) {
        this.id = id;
        sessions = new ArrayList<>();
        activitySessions = new ArrayList<>();
        quizSessions = new ArrayList<>();
        forumSessions = new ArrayList<>();
        videoSessions = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void addQuizSession(Session s) {
        quizSessions.add(s);
    }

    public List<Session> getQuizSessions () {
        return quizSessions;
    }

    public void setQuizSessions(List<Session> quizSessions) {
        this.quizSessions = quizSessions;
    }

    public void addForumSession(Session s) {
        forumSessions.add(s);
    }

    public List<Session> getForumSessions () {
        return forumSessions;
    }

    public void setForumSessions(List<Session> forumSessions) {
        this.forumSessions = forumSessions;
    }

    public void addVideoSession(Session s) {
        videoSessions.add(s);
    }

    public List<Session> getVideoSessions () {
        return videoSessions;
    }

    public void setVideoSessions(List<Session> videoSessions) {
        this.videoSessions = videoSessions;
    }

    public void addSession(Session s) {
        sessions.add(s);
    }

    public List<Session> getSessions () {
        return sessions;
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
    }

    public List<Session> getActivitySessions() {
        return this.activitySessions;
    }

    public void setActivitySessions(List<Session> sessions) {
        this.activitySessions = sessions;
    }
}
