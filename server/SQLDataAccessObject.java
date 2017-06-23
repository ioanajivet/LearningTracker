import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SQLDataAccessObject {

    private Connection sqlDBConnection = null;

    public SQLDataAccessObject(DatabaseData databaseData) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        Class.forName(databaseData.dbDriver).newInstance();
        sqlDBConnection = DriverManager.getConnection(databaseData.dbURL, databaseData.dbUser, databaseData.dbPass);
    }

    public void closeConnectionQuietly() {
        try {
            if(sqlDBConnection != null)
                sqlDBConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //===================
    //===== Utils =======
    //===================

    public String getCourseStartDate() throws SQLException {
        String query = "SELECT * FROM courses;";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        String startDate = "";

        if (res.next())
            startDate = res.getString("start_time");

        return startDate;
    }

    public String getCourseEndDate() throws SQLException {
        String query = "SELECT * FROM courses;";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        String endDate = "";

        if (res.next())
            endDate = res.getString("end_time");

        return endDate;
    }

    public double[] getDataSeries(long week, String table) throws SQLException {
        double[] profile = new double[6];

        String query = "SELECT * FROM " + table + " WHERE week='" + week + "';";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        if (res.next()) {
            for (int i = 1; i <= 6; i++)
                profile[i - 1] = res.getInt("metric_" + i);
        }

        return profile;
    }

    //============================================================================
    //===== Methods used for the metric calculation for individual learners ======
    //============================================================================

    // getTimeOnPlatform: time spent on the platform calculated summing up the duration of each session
    // Unit: seconds
    // Tables used: "sessions" with the fields:
    // 		"learner_id" - to identify sessions belonging to the current user
    // 		"duration" - the duration of each session

    public int getTimeOnPlatform(String userId) throws SQLException {
        String query = "SELECT SUM(duration) AS duration FROM sessions WHERE course_learner_id='" + userId + "';";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        int total_duration = 0;

        if (res.next())
            total_duration = res.getInt("duration");

        return total_duration;
    }

    // getForumContributions: number of contributions to the forum
    // Unit: -
    // Tables used: "forum_interaction" with the fields:
    //		"course_run_id" - to identify the course
    // 		"learner_id" - to identify the current user
    // 		"post_id" - to calculate the number of user's forum posts

    public int getForumContributions(String userId) throws SQLException {
        String query = "SELECT COUNT(post_id) AS post_count FROM forum_interaction WHERE course_learner_id='" + userId + "';";

        Statement stmt = sqlDBConnection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        int forum_contribution = 0;

        if (rs.next())
            forum_contribution = rs.getInt("post_count");

        return forum_contribution;
    }

    // getQuizAttempted: number of unique quiz questions that were attempted by a learner
    // Unit: -
    // Tables used: "submissions" with the fields of:
    //		"course_run_id" - to identify the course
    // 		"learner_id" - to identify the current user
    // 		"question_id" - to calculate the number of user's quiz attempts

    public int getQuizAttempted(String userId) throws SQLException {
        String query = "SELECT COUNT(DISTINCT question_id) AS quiz_count FROM submissions "
                + "WHERE course_learner_id='" + userId + "';";

        Statement stmt = sqlDBConnection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        int quiz_attempts = 0;

        if (rs.next())
            quiz_attempts += rs.getInt("quiz_count");

        return quiz_attempts;
    }

    // getTimeOnQuiz: time spent on quiz pages calculated by summing up the duration of each quiz_session
    // Unit: seconds
    // Tables used: "quiz_sessions" with the fields:
    //		"course_run_id" - to identify the course
    // 		"learner_id" - to identify sessions belonging to the current user
    // 		"duration" - the duration of each quiz session

    public int getTimeOnQuiz(String userId) throws SQLException {
        String query = "SELECT SUM(duration) AS quiz_duration FROM quiz_sessions WHERE course_learner_id='" + userId + "';";

        Statement stmt = sqlDBConnection.createStatement();
        ResultSet rs;

        rs = stmt.executeQuery(query);

        int quiz_duration = 0;

        if (rs.next())
            quiz_duration = rs.getInt("quiz_duration");

        return quiz_duration;
    }


    // timeliness: how many hours before the deadline learners submit their quiz question answers on average
    // Unit: hours
    // Tables used: "submissions" & ""quiz_questions". By which the "submission" used fields are:
    //		"course_run_id" - to identify the course
    // 		"learner_id" - to identify the current user
    //		"question_id" - to identify the current question
    //		"submission_timestamp" - to identify the user's quiz submission date
    //		*and the "quiz_questions" table used fields are:
    //		"question_due" - to identify the quiz question due date
    //		"question_id" - to identify the current question

    public int getTimeliness(String userId) throws SQLException {
        String query = "SELECT COUNT(*) AS countLines"
                + ", AVG(TIME_TO_SEC(TIMEDIFF(submissions.sumission_timestamp, quiz_questions.question_due)) / 3600) AS timeliness_average"
                + " FROM submissions"
                + " INNER JOIN"
                + " quiz_questions ON submissions.question_id = quiz_questions.question_id WHERE "
                + "submissions.course_learner_id= '" + userId + "';";

        Statement stmt = sqlDBConnection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        double timeliness_average = 0;

        if (rs.next())
            timeliness_average += rs.getInt("timeliness_average");

        return (int) Math.round(timeliness_average);
    }


    //getWatchedVideosWithDuration: returns a Map with pairs (video_id, duration) where:
    // - video_id = the id of a video watched by the learner
    // - duration = time spent watching this video
    // Tables used: "video_interaction" with the fields:
    //		"course_run_id" - to identify the course
    // 		"learner_id" - to identify the current user
    //		"duration" - to examine if a user wathces more than or equal 80% of the total video duration

    public Map<String,Integer> getWatchedVideosWithDuration(String userId) throws SQLException {
        Map<String, Integer> videosWatchedDuration = new HashMap<>();

        String query = "SELECT video_id, SUM(duration) AS duration FROM video_interaction WHERE"
                + " course_learner_id='" + userId + "'  GROUP BY video_id;";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        while (res.next())
            videosWatchedDuration.put(res.getString("video_id"), res.getInt("duration"));

        return videosWatchedDuration;

    }

    //getWatchedVideosCount: returns a Map with pairs (video_id, count) where:
    // - video_id = the id of a video watched by the learner
    // - count = how many times a video has been accessed
    // Tables used: "video_interaction" with the fields:
    //		"course_run_id" - to identify the course
    // 		"learner_id" - to identify the current user

    public Map<String,Integer> getWatchedVideosCount(String userId) throws SQLException {
        Map<String, Integer> videosWatchedCount = new HashMap<>();

        String query = "SELECT video_id, COUNT(video_id) AS watch_count FROM video_interaction"
                + " WHERE course_learner_id='" + userId + "' GROUP BY video_id;";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        while (res.next())
            videosWatchedCount.put(res.getString("video_id"), res.getInt("watch_count"));

        return videosWatchedCount;

    }

    //getVideosLengths: returns a Map with pairs (video_id, length) where:
    // - video_id = the id of a video watched by the learner
    // - length = the length of the video
    //  Tables used: "video_additional" with the fields:
    //      "course_run_id" - to identify the current course
    // 		"video_id" - video ID
    // 		"length" - length of a video

    public Map<String,Integer> getVideosLengths() throws SQLException {
        Map<String, Integer> videosLength = new HashMap<>();

        String query = "SELECT video_id, length FROM video_additional;";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        while (res.next())
            videosLength.put(res.getString("video_id"), res.getInt("length"));

        return videosLength;

    }

    //=======================================================
    //===== Metric calculation for graduate learners ========
    //=======================================================
    // NOT REQUIRED IF WE USE THE DATA FROM 2015

    //todo: metric 2
    // Metric 2: LecturesRevisited: number of videos that have been visited more than once and viewed more than 80% of their total duration
    // Unit: -


    // Metric 3: ForumActivity: number of contributions to the forum
    // Unit: -

    public Map<Integer, Double> getLearnerForumContributions(String course_id, String weekEndDate) throws SQLException {
        Map<Integer, Double> forumContributionsByLearner = new HashMap<>();

        String query = "SELECT course_learner.learner_id, count(course_forum_interaction.post_id) as posts\n" +
                "  FROM course_learner \n" +
                "    LEFT JOIN (\n" +
                "      (SELECT learner_id, post_id FROM forum_interaction \n" +
                "         WHERE forum_interaction.course_run_id = " + course_id + " AND post_timestamp<='" + weekEndDate + "'\n" +
                "        ) AS course_forum_interaction)\n" +
                "    ON course_learner.learner_id = course_forum_interaction.learner_id \n" +
                "       WHERE course_learner.final_grade>=0.6\n" +
                "          GROUP BY course_learner.learner_id;";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        while (res.next())
            forumContributionsByLearner.put(res.getInt("learner_id"), res.getDouble("posts"));

        return forumContributionsByLearner;
    }

    // Metric 4: QuizAttempted: number of unique quiz questions that were attempted by a learner
    // Unit: -

    public Map<Integer, Double> getLearnerQuizAttempted(String course_id, String weekEndDate) throws SQLException {
        Map<Integer, Double> quizAttemptedByLearner = new HashMap<>();

        String query = "SELECT course_learner.learner_id, count(distinct question_id) AS quizzes\n" +
                "    FROM course_learner LEFT JOIN \n" +
                "      (SELECT learner_id, question_id FROM submissions\n" +
                "            WHERE submissions.course_run_id = " + course_id + " AND submission_timestamp<='" + weekEndDate + "'\n" +
                "        ) AS course_submissions\n" +
                "    ON course_learner.learner_id = course_submissions.learner_id \n" +
                "       WHERE course_learner.final_grade>=0.6\n" +
                "         GROUP BY course_learner.learner_id;";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        while (res.next())
            quizAttemptedByLearner.put(res.getInt("learner_id"), res.getDouble("quizzes"));

        return quizAttemptedByLearner;
    }

    //todo: metric 5
    // Metric 5: ProportionTimeOnQuiz: the proportion of time spent on quiz pages from the total time spent on the platform
    // Unit: -

    public Map<Integer, Double> getLearnerTimeOnPlatform(String course_id, String weekEndDate) throws SQLException {
        Map<Integer, Double> timeOnPlatformByLearner = new HashMap<>();

        String query = "SELECT course_learner.learner_id, sum(course_sessions.duration) AS duration\n" +
                "  FROM course_learner \n" +
                "    LEFT JOIN (\n" +
                "      (SELECT learner_id, duration FROM sessions \n" +
                "         WHERE sessions.course_run_id = " + course_id + " AND start_time<='" + weekEndDate + "'\n" +
                "        ) AS course_sessions)\n" +
                "    ON course_learner.learner_id = course_sessions.learner_id \n" +
                "       WHERE course_learner.final_grade>=0.6\n" +
                "          GROUP BY course_learner.learner_id;";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        while (res.next())
            timeOnPlatformByLearner.put(res.getInt("learner_id"), res.getDouble("duration"));

        return timeOnPlatformByLearner;
    }

    public Map<Integer, Double> getLearnerTimeOnQuiz(String course_id, String weekEndDate) throws SQLException {
        Map<Integer, Double> timeOnPlatformByLearner = new HashMap<>();

        String query = "SELECT course_learner.learner_id, sum(course_quiz_sessions.duration) as duration\n" +
                "  FROM course_learner \n" +
                "    LEFT JOIN (\n" +
                "      (SELECT learner_id, duration FROM quiz_sessions \n" +
                "         WHERE quiz_sessions.course_run_id = " + course_id + " AND start_time<='" + weekEndDate + "'\n" +
                "        ) AS course_quiz_sessions)\n" +
                "    ON course_learner.learner_id = course_quiz_sessions.learner_id \n" +
                "       WHERE course_learner.final_grade>=0.6\n" +
                "          GROUP BY course_learner.learner_id;";

        Statement st = sqlDBConnection.createStatement();
        ResultSet res = st.executeQuery(query);

        while (res.next())
            timeOnPlatformByLearner.put(res.getInt("learner_id"), res.getDouble("duration"));

        return timeOnPlatformByLearner;
    }

    //todo: metric 6

}
