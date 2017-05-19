import javafx.util.Pair;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by Ioana on 4/7/2016.
 */
public class UserMetricComputation {

    private String id;

    private HashMap<Integer, List<Session>> sessions;
    private HashMap<Integer, List<VideoSession>> videoSessions;
    private HashMap<Integer, List<ForumSession>> forumSessions;
    private HashMap<Integer, List<QuizSession>> quizSessions;
    private HashMap<Integer, List<Submission>> quizSubmissions;
    private HashMap<Integer, Integer> collaborations;

    public UserMetricComputation(String id) {
        this.id =  id;

        sessions = new HashMap<>();
        videoSessions = new HashMap<>();
        forumSessions = new HashMap<>();
        quizSessions = new HashMap<>();
        quizSubmissions = new HashMap<>();
        collaborations = new HashMap<>();

    }

    //Methods for adding data
    public void addSession(int week, Session session) {
        List<Session> sessionList = sessions.get(week);

        if(sessionList == null) {
            sessionList = new ArrayList<>();
            sessions.put(week, sessionList);
        }

        sessionList.add(session);
    }

    public void addForumSession(int week, ForumSession forumSession) {
        List<ForumSession> forumSessionList = forumSessions.get(week);

        if(forumSessionList == null) {
            forumSessionList = new ArrayList<>();
            forumSessions.put(week, forumSessionList);
        }

        forumSessionList.add(forumSession);
    }

    public void addQuizSession(int week, QuizSession quizSession) {
        List<QuizSession> quizSessionList = quizSessions.get(week);

        if(quizSessionList == null) {
            quizSessionList = new ArrayList<>();
            quizSessions.put(week, quizSessionList);
        }

        quizSessionList.add(quizSession);
    }

    public void addSubmission(int week, Submission submission) {
        List<Submission> submissionList = quizSubmissions.get(week);

        if(submissionList == null) {
            submissionList = new ArrayList<>();
            quizSubmissions.put(week, submissionList);
        }

        submissionList.add(submission);
    }

    public void addVideoSession(int week, VideoSession videoSession){
        List<VideoSession> videoSessionList = videoSessions.get(week);

        if(videoSessionList == null) {
            videoSessionList = new ArrayList<>();
            videoSessions.put(week, videoSessionList);
        }

        videoSessionList.add(videoSession);
    }

    public void addCollaboration(int week) {
        if(collaborations.containsKey(week))
            collaborations.put(week, collaborations.get(week) + 1);
        else
            collaborations.put(week, 1);
    }

    //Methods for calculating metrics
    //Metric 1: Sessions per week
    public int getSessionsPerWeek(int week) {
        int sessionCount = getSessions(week);

        if(sessionCount == 0)
            return 0;

        return (int) Math.round(sessionCount * 1.0/week);
    }

    //Metric 2: Average length of a session - min
    public int getAverageSessionLength(int week) {
        int sessionCount = getSessions(week);
        int sessionTime = getTimeOnPlatform(week);

        if(sessionCount == 0)
            return 0;

        return (int) Math.round(sessionTime/(sessionCount * 60.0));
    }

    //Metric 3: Average time between sessions - h
    // if the time between sessions <1h, it is not calculated
    public int getAverageTimeBetweenSessions(int week) {
        List<Session> sessionTimestamps = new ArrayList<>();
        int i, skipped = 0;
        long time_ms = 0, diff;

        for(i = 1; i <= week; i++)
            if(sessions.containsKey(i))
                sessionTimestamps.addAll(sessions.get(i));

        if (sessionTimestamps.size() < 2)
            return 0;

        Collections.sort(sessionTimestamps);

        for(i = 1; i < sessionTimestamps.size(); i++) {

            diff = sessionTimestamps.get(i).getStartDate().getTime() - sessionTimestamps.get(i-1).getEndDate().getTime();

            //if there is less than 1h in between the sessions, they are considered the same one
            if(diff >= 3600000)
                time_ms += diff;
            else
                skipped++;
        }

        //if there is only one session block the difference between the consecutive sessions is infinite
        if(i-1 == skipped)
            return 0;

        return (int) Math.round(TimeUnit.MILLISECONDS.toHours(time_ms)*1.0/(i-1-skipped));

    }

    //Metric 4. Forum sessions
    public int getForumSessions(int week) {
        int forumSessionCount = 0;

        for(int i = 1; i <= week; i++)
            if(forumSessions.containsKey(i))
                forumSessionCount += forumSessions.get(i).size();

        return forumSessionCount;
    }

    //Metric 5. Quiz answers submitted
    public int getQuizAttempted(int week) {
        List<Submission> allSubmissions = new ArrayList<>();

        for(int i = 1; i <= week; i++)
            if(quizSubmissions.containsKey(i))
                allSubmissions.addAll(quizSubmissions.get(i));

        return (int) allSubmissions.stream().map(s -> s.getProblem()).distinct().count();
    }

    //Metric 6. Timeliness according to the actual deadline = end of the course
    public int getTimeliness(int week){
        List<Submission> allSubmissions = new ArrayList<>();
        List<String> attemptedProblems;
        List<Pair<String, Long>> problemTimeliness, minimumProblemTimeliness = new ArrayList<>();

        for(int i = 1; i <= week; i++)
            if(quizSubmissions.containsKey(i))
                allSubmissions.addAll(quizSubmissions.get(i));

        //select the problems that were attempted
        attemptedProblems = allSubmissions.stream()
                .map(s -> s.getProblem())
                .distinct()
                .collect(Collectors.toList());

        //map each submission to a pair: problem_id - time until the deadline
        problemTimeliness = allSubmissions.stream()
                .map(s -> new Pair<>(s.getProblem(), s.getTimeliness()))
                .collect(Collectors.toList());

        //select the most recent submission for each problem
        for (String problem: attemptedProblems) {
            long minimum = problemTimeliness.stream()
                    .filter(p -> p.getKey().compareTo(problem) == 0)
                    .mapToLong(p -> p.getValue())
                    .min()
                    .getAsLong();

            minimumProblemTimeliness.add(new Pair<>(problem, minimum));
        }

        //average the minimum timeliness
        OptionalDouble timeliness = minimumProblemTimeliness.stream()
                .mapToLong(p -> p.getValue())
                .average();

        if (timeliness.isPresent())
            return (int) Math.round(timeliness.getAsDouble());

        return 0;
    }

    //Metric 7: Number of sessions logged
    public int getSessions(int week) {
        int sessionCount = 0;

        for(int i = 1; i <= week; i++)
            if(sessions.containsKey(i))
                sessionCount += sessions.get(i).size();

        return sessionCount;
    }

    //Metric 8: Number of videos accessed
    public int getVideosAccessed(int week) {
        List<VideoSession> allVideoSession = new ArrayList<>();

        for(int i = 1; i <= week; i++)
            if(videoSessions.containsKey(i))
                allVideoSession.addAll(videoSessions.get(i));

        return (int) allVideoSession.stream().map(e -> e.getVideoId()).distinct().count();
    }

    //Metric 9: Time on the platform
    public int getTimeOnPlatform(int week) {
        int timeOnPlatform = 0;

        for(int i = 1; i <= week; i++) {
            if(sessions.containsKey(i))
                timeOnPlatform += sessions.get(i).stream().mapToInt(s -> s.getDuration()).sum();
        }

        return timeOnPlatform;
    }

    //Metric 10: Average time per week
    public int getAverageTimePerWeek(int week) {

        return getTimeOnPlatform(week)/week;
    }

    //Metric 11: Proportion of time spent on assessments
    public int getProportionTimeOnQuiz(int week) {
        int timeOnPlatform = getTimeOnPlatform(week);
        int timeOnQuiz = 0;

        for(int i = 1; i <= week; i++) {
            if(quizSessions.containsKey(i))
                timeOnQuiz += quizSessions.get(i).stream().mapToInt(s -> s.getDuration()).sum();
        }

        if(timeOnPlatform == 0)
            return 0;
        return timeOnQuiz * 100 / timeOnPlatform;
    }

    //Metric 12: # Video lectures revisited
    public int getLecturesRevisited(int week) {
        List<String> videosAccessed = new ArrayList<>();
        List<VideoSession> allVideoSessions = new ArrayList<>();
        int revisited = 0;

        for(int i = 1; i <= week; i++)
            if(videoSessions.containsKey(i))
                allVideoSessions.addAll(videoSessions.get(i));

        //get a list of all the videos watched
        videosAccessed.addAll(allVideoSessions
                .stream()
                .map(e -> e.getVideoId())
                .distinct()
                .collect(Collectors.toList()));

        //for each video - check if it more than 80% and viewed multiple times
        for (String video: videosAccessed) {
            List<VideoSession> videoSessionsForVideo = allVideoSessions.stream()
                    .filter(e -> video.compareTo(e.getVideoId()) == 0)
                    .collect(Collectors.toList());

            if (videoSessionsForVideo.size() < 2)
                continue;

            int timeViewed = videoSessionsForVideo.stream().mapToInt(e -> e.getDuration()).sum();
            int videoLength = videoSessionsForVideo.get(0).getVideoLength();

            if(timeViewed * 100 / videoLength >= 80)
                revisited++;
        }

        return revisited;

    }

    //Metric 13: # activities in the forum
    public int getForumActivity(int week){
        int collabs = 0;

        for(int i = 1; i <= week; i++)
            if(collaborations.containsKey(i))
                collabs += collaborations.get(i);

        return collabs;
    }

    public String getId() { return this.id; }

}
