
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created by Ioana on 4/7/2016.
 */
public class MetricComputation {

    public enum Edition { PREVIOUS, CURRENT};

    static HashMap<String, UserMetricComputation> users;
    static List<String> graduates;
    static HashMap<String, Integer> problems;
    static HashMap<String, Integer> videoLengths;
    static HashMap<Integer, Integer> maximums;

    static HashMap<Integer, Double> thresholds;

    static HashMap<Integer, BiFunction<UserMetricComputation, Integer, Integer>> metricMethods;

    public static void computeMetrics(String course, int week, String path) throws IOException, ParseException {
        String thresholdPath = path + "thresholds\\";
        String dataPath = path + "week" + week + "\\data\\";
        String metricsPath = path + "week" + week + "\\metrics\\";
        Edition edition = Edition.CURRENT;

        System.out.println("Initializing course for week " + week + " ... " + course.toUpperCase());
        initialize();

        System.out.println("Computing study sittings for week " + week + " ... " + course.toUpperCase());
        Sitting.computeSittings(path + "learners.csv", dataPath);

        System.out.println("Loading data for week " + week + "... " + course.toUpperCase());

        readUsers(users, path + "learners.csv");
        readVideoLengths(path + "videos.csv");
        readProblems(path + "problems.csv");

        loadData(edition, dataPath);

        readMaximums(thresholdPath + "maximums.csv", week);
        readScaledThresholds(thresholdPath + "scaled_thresholds.csv", week);

        System.out.println("Writing metrics for week " + week + "... " + course.toUpperCase());
        writeMetrics(users, week, metricsPath, metricsPath + "\\" + course.toUpperCase() + "_metrics.csv");

        System.out.println("Writing scaled metrics for week " + week + "... " + course.toUpperCase());
        writeScaledMetrics(users, week, metricsPath, metricsPath + "\\" + course.toUpperCase() + "_scaled_metrics.csv");

        System.out.println("Writing file for database upload for week " + week + "... " + course.toUpperCase());
        writeFileForDatabaseUpload(users, week, metricsPath, metricsPath + "\\" + course.toUpperCase() + "_week" + week + "_for_database.csv");
    }

    public static void computeThreshold(String course, int week, String path, int cutOffPercent) throws IOException, ParseException {
        String thresholdPath = path + "thresholds\\";
        String dataPath = path + "2015\\";
        Edition edition = Edition.PREVIOUS;

        System.out.println("Initializing course... " + course.toUpperCase());
        initialize();

        System.out.println("Computing study sittings for week " + week + " ... " + course.toUpperCase());
        Sitting.computeSittings(dataPath + "graduates.csv", dataPath);

        System.out.println("Loading data... " + course.toUpperCase());
        readUsers(users, dataPath + "graduates.csv");
        readVideoLengths(dataPath + "videos.csv");
        readProblems(dataPath + "problems.csv");

        loadData(edition, dataPath);

        System.out.println("Writing average graduate values... " + course.toUpperCase());
        writeThresholds(week, thresholdPath, "thresholds.csv", cutOffPercent);

        //write time on platform
        //writeTime("data\\precalc\\timeeees.csv");

        System.out.println("Writing maximums... " + course.toUpperCase());
        writeMaximums(week, thresholdPath, "maximums.csv", cutOffPercent);

        System.out.println("Writing average graduate scaled values... " + course.toUpperCase());
        writeScaledThresholds(week, thresholdPath, "scaled_thresholds.csv", cutOffPercent);
    }

    private static void writeTime(String s) throws IOException {
        CSVWriter output = new CSVWriter(new FileWriter(s), ',');
        String[] toWrite;

        toWrite = "User_id#Average time/week".split("#");

        output.writeNext(toWrite);

        for(Map.Entry<String, UserMetricComputation> entry: users.entrySet()) {
            toWrite[0] = entry.getKey();
            toWrite[1] = String.valueOf(entry.getValue().getTimeOnPlatform(12) / 3600.0);

            output.writeNext(toWrite);
        }

        output.close();
    }

    private static void initialize(){
        users = new HashMap<>();
        graduates = new ArrayList<>();
        problems = new HashMap<>();
        maximums = new HashMap<>();
        videoLengths = new HashMap<>();
        thresholds = new HashMap<>();

        metricMethods = new HashMap<>();
        metricMethods.put(1, UserMetricComputation::getAverageTimePerWeek);
        metricMethods.put(2, UserMetricComputation::getLecturesRevisited);
        metricMethods.put(3, UserMetricComputation::getForumActivity);
        metricMethods.put(4, UserMetricComputation::getQuizAttempted);
        metricMethods.put(5, UserMetricComputation::getProportionTimeOnQuiz);
        metricMethods.put(6, UserMetricComputation::getTimeliness);
    }

    //READ
    private static void loadData(Edition edition, String dataPath) throws IOException, ParseException {

        //Utils.splitIntoWeeks(startDate, endDate);
        readSittings(edition, dataPath + "sittings.csv");

        readForumSessions(edition, dataPath + "forum_sessions.csv");

        readQuizSessions(edition, dataPath + "\\learning_activities\\new_quiz_sessions.csv");

        readObservations(edition, dataPath + "video_interaction.csv");

        readSubmissions(edition, dataPath + "submissions.csv");

        readCollaborations(edition, dataPath + "forum_interaction.csv");

    }

    //Reads from a file with ids on the first column and creates a Hashmap of UserMetricComputation objects with the id as key
    private static void readUsers(HashMap<String, UserMetricComputation> group, String filename) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String [] nextLine;

        csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null)
            group.put(nextLine[0], new UserMetricComputation(nextLine[0]));

        csvReader.close();

        System.out.println("Learners read: " + users.size());
    }

    private static void readSittings(Edition edition, String filename) throws IOException, ParseException {
        //User_id#Start time#End time#Duration
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String[] nextLine;
        int week, duration;
        String user_id, start, end;

        nextLine = csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null) {
            user_id = nextLine[0];
            start = nextLine[1];
            end = nextLine[2];
            duration = Integer.parseInt(nextLine[3]);

            if(users.containsKey(user_id)) {
                week = Utils.getWeek(edition, start);

                if(week == 99)
                    continue;

                users.get(user_id).addSession(week, new Session(user_id, start, end, duration));

            }
        }

        csvReader.close();
    }

    private static void readForumSessions(Edition edition, String filename) throws IOException, ParseException {
        //session_id, course_user_id, ??, start_time, end_time, duration
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String[] nextLine;
        int duration, week, read = 0;
        String userId, start, end;

        //nextLine = csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null) {
            userId = nextLine[1];
            start = nextLine[3];
            end = nextLine[4];
            duration = Integer.parseInt(nextLine[5]);

            if(users.containsKey(userId)) {
                week = Utils.getWeek(edition, start);
                if(week == 99) {
                    System.out.println("99");
                    continue;
                }

                users.get(userId).addForumSession(week, new ForumSession(userId, start, end, duration));

                read++;

            }
        }

        csvReader.close();

        System.out.println("Forum visits read: " + read);
    }

    private static void readQuizSessions(Edition edition, String filename) throws IOException, ParseException {
        //session_id, course_user_id, start_time, end_time, duration
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String[] nextLine;
        int duration, week, read = 0;
        String userId, start, end;

        nextLine = csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null) {
            userId = nextLine[1];
            start = nextLine[2];
            end = nextLine[3];
            duration = Integer.parseInt(nextLine[4]);

            if(users.containsKey(userId)) {
                week = Utils.getWeek(edition, start);

                if(week == 99)
                    continue;

                users.get(userId).addQuizSession(week, new QuizSession(userId, start, end, duration));

                read++;

            }
        }

        csvReader.close();

        System.out.println("Quiz sessions read: " + read);
    }

    private static void readVideoLengths(String filename) throws IOException, ParseException {
        //video_id, user_id, length, week
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String[] nextLine;
        int length, week, read = 0;
        String videoId;

        while ((nextLine = csvReader.readNext()) != null) {
            videoId = nextLine[0];
            length = Integer.parseInt(nextLine[1]);

            videoLengths.put(videoId, length);

            read++;

        }

        csvReader.close();

        System.out.println("Videos read: " + read);
    }

    private static void readProblems(String filename) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String [] nextLine;
        int week;
        String problemId;

        while ((nextLine = csvReader.readNext()) != null) {
            problemId = nextLine[0];
            week = Integer.parseInt(nextLine[2]);
            problems.put(problemId, week);
        }

        csvReader.close();

        System.out.println("Problems read: " + problems.size());
    }

    private static void readSubmissions(Edition edition, String filename) throws IOException, ParseException {
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String [] nextLine;
        UserMetricComputation user;
        int week;
        String user_id, problem_id, timestamp, deadline;
        int sub = 0;

        while ((nextLine = csvReader.readNext()) != null) {
            user_id = nextLine[1];
            problem_id = nextLine[2].split("\\@")[2];
            //problem_id = nextLine[2];
            timestamp = Utils.formatTimestamp(nextLine[3]);

            user = users.get(user_id);
            if (user == null)    //user are not in the active base -> ignore submission
                continue;

            if(!problems.containsKey(problem_id))   //ignore problems that are not graded
                continue;


            week = Utils.getWeek(edition, timestamp);
            if(week == 99)
                continue;

            deadline = Utils.getProblemDeadline(edition);
            user.addSubmission(week, new Submission(user_id, problem_id, timestamp, deadline));

            sub++;
        }

        csvReader.close();

        System.out.println("Submissions read: " + sub);
    }

    private static void readCollaborations(Edition edition, String filename) throws IOException, ParseException {
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String [] nextLine;
        UserMetricComputation user;
        int week;
        String user_id, timestamp;
        int sub = 0;

        //nextLine = csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null) {
            user_id = nextLine[1];
            timestamp = Utils.formatTimestamp(nextLine[5]);

            user = users.get(user_id);
            if (user == null)    //user are not in the active base -> ignore submission
                continue;

            week = Utils.getWeek(edition, timestamp);
            if(week == 99)
                continue;

            user.addCollaboration(week);

            sub++;
        }

        csvReader.close();

        System.out.println("Collaborations read: " + sub);
    }

    private static void readObservations(Edition edition, String filename) throws IOException, ParseException {
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String [] nextLine;
        UserMetricComputation user;
        int week, duration, length;
        String user_id, video_id, start, end;
        int sub = 0;

        while ((nextLine = csvReader.readNext()) != null) {
            user_id = nextLine[1];

            user = users.get(user_id);
            if (user == null)    //user are not in the active set -> ignore observation
                continue;

            //video_id = nextLine[2].split("/")[5];
            video_id = nextLine[2];
            duration = Integer.parseInt(nextLine[3]);
            start = nextLine[12];
            end = nextLine[13];

            if(!videoLengths.containsKey(video_id))
                continue;

            length = videoLengths.get(video_id);

            week = Utils.getWeek(edition, start);
            if(week == 99)
                continue;

            //String problemId, int submissionWeek, Date submissionTime, Date problemDeadline
            user.addVideoSession(week, new VideoSession(user_id, video_id, start, end, duration, length));
            sub++;
        }

        csvReader.close();

        System.out.println("Observations read: " + sub);
    }

    private static void readMaximums(String filename, int week) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String [] nextLine;
        int line = 1;

        csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null && line < week) {
            line++;
        }

        for(int i = 1; i <= metricMethods.size(); i++)
            maximums.put(i, Integer.parseInt(nextLine[i]));

        csvReader.close();
    }

    private static void readScaledThresholds(String filename, int week) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(filename));
        String [] nextLine;
        int line = 1;

        csvReader.readNext();

        while ((nextLine = csvReader.readNext()) != null && line < week) {
            line++;
        }

        for(int i = 1; i <= metricMethods.size(); i++)
            thresholds.put(i, Double.parseDouble(nextLine[i]));

        csvReader.close();
    }

    //WRITE
    private static void writeMetrics(HashMap<String, UserMetricComputation> group, int week, String metricsPath, String filename) throws IOException {

        Utils.checkForDirectory(metricsPath);

        CSVWriter output = new CSVWriter(new FileWriter(filename), ',');
        String[] toWrite;
        UserMetricComputation current;

        toWrite = "User_id#Average time/week#Revisited video-lectures#Forum contributions#Assessment questions attempted#% time on assessments#Timeliness".split("#");

        output.writeNext(toWrite);

        for (Map.Entry<String, UserMetricComputation> entry : group.entrySet()) {
            current = entry.getValue();
            toWrite[0] = entry.getKey();

            for(int i = 1; i <= metricMethods.size(); i++)
                toWrite[i] = String.valueOf(metricMethods.get(i).apply(current, week));

            output.writeNext(toWrite);
        }
        output.close();
    }

    private static void writeScaledMetrics(HashMap<String, UserMetricComputation> group, int week, String metricsPath, String filename) throws IOException {
        Utils.checkForDirectory(metricsPath);

        CSVWriter output = new CSVWriter(new FileWriter(filename), ',');
        String[] toWrite;
        UserMetricComputation current;

        toWrite = "User_id#Average time/week#Revisited video-lectures#Forum contributions#Assessment questions attempted#% time on assessments#Timeliness".split("#");

        output.writeNext(toWrite);

        for (Map.Entry<String, UserMetricComputation> entry : group.entrySet()) {
            current = entry.getValue();
            toWrite[0] = entry.getKey();

            for(int i = 1; i <= metricMethods.size(); i++) {
                toWrite[i] = String.format("%.1f", ScalingComputation.scaleMetricValue(metricMethods.get(i).apply(current, week), maximums.get(i)));

            }

            output.writeNext(toWrite);
        }
        output.close();
    }

    private static void writeMaximums(int week, String thresholdsPpath, String filename, int cutOffPercent) throws IOException {
        Utils.checkForDirectory(thresholdsPpath);

        CSVWriter output = new CSVWriter(new FileWriter(thresholdsPpath + filename), ',');
        String[] toWrite;

        toWrite = "User_id#Average time/week#Revisited video-lectures#Forum contributions#Assessment questions attempted#% time on assessments#Timeliness".split("#");

        output.writeNext(toWrite);

        for(int i = 1; i <= week; i++) {
            toWrite[0] = "Week " + i;

            for(int j = 1; j <= metricMethods.size(); j++)
                toWrite[j] = String.valueOf(AverageGraduateComputation.getMaximumInCutOffRange(users, metricMethods.get(j), i, cutOffPercent));

            output.writeNext(toWrite);
        }

        output.close();
    }

    private static void writeThresholds(int week, String thresholdsPath, String filename, int cutOffPercent) throws IOException {
        Utils.checkForDirectory(thresholdsPath);

        CSVWriter output = new CSVWriter(new FileWriter(thresholdsPath + filename), ',');
        String[] toWrite;

        toWrite = "User_id#Average time/week#Revisited video-lectures#Forum contributions#Assessment questions attempted#% time on assessments#Timeliness".split("#");

        output.writeNext(toWrite);

        for(int i = 1; i <= week; i++) {
            toWrite[0] = "Week " + i;
            toWrite[1] = String.format("%.1f" , AverageGraduateComputation.getAverage(users, metricMethods.get(1), i, cutOffPercent) / 3600.0);

            for(int j = 2; j <= metricMethods.size() ; j++)
                toWrite[j] = String.valueOf(AverageGraduateComputation.getAverage(users, metricMethods.get(j), i, cutOffPercent));

            output.writeNext(toWrite);
        }

        output.close();
    }

    private static void writeScaledThresholds(int week, String thresholdPaths, String filename, int cutOffPercent) throws IOException {
        Utils.checkForDirectory(thresholdPaths);

        CSVWriter output = new CSVWriter(new FileWriter(thresholdPaths + filename), ',');
        String[] toWrite;

        toWrite = "User_id#Average time/week#Revisited video-lectures#Forum contributions#Assessment questions attempted#% time on assessments#Timeliness".split("#");

        output.writeNext(toWrite);

        for(int i = 1; i <= week; i++) {
            toWrite[0] = "Week " + i;

            for(int j = 1; j <= metricMethods.size() ; j++)
                toWrite[j] = String.format("%.1f", ScalingComputation.getScaledThresholdValue(users, metricMethods.get(j), i, cutOffPercent));

            output.writeNext(toWrite);
        }

        output.close();
    }

    private static void writeFileForDatabaseUpload(HashMap<String, UserMetricComputation> group, int week, String metricsPath, String filename) throws IOException {

        Utils.checkForDirectory(metricsPath);

        CSVWriter output = new CSVWriter(new FileWriter(filename), ',');
        String[] toWrite;
        UserMetricComputation current;

        toWrite = "Week#User_id#Activity score#Average time/week#Revisited video-lectures#Forum contributions#Assessment questions attempted#% time on assessments#Timeliness#Scaled 1#Scaled 2#Scaled 3#Scaled 4#Scaled 5#Scaled 6".split("#");

        output.writeNext(toWrite);

        for (Map.Entry<String, UserMetricComputation> entry : group.entrySet()) {
            current = entry.getValue();

            toWrite[0] = String.valueOf(week);
            toWrite[1] = entry.getKey().split("_")[1];
            toWrite[2] = String.format("%.2f" ,computeStatus(current, week));

            toWrite[3] = String.format("%.1f", current.getAverageTimePerWeek(week) / 3600.0);
            //write metrics
            for(int i = 2; i <= metricMethods.size(); i++)
                toWrite[i+2] = String.valueOf(metricMethods.get(i).apply(current, week));

            //convert hours to days for timeliness
            toWrite[8] = String.valueOf((int) Math.round(Integer.parseInt(toWrite[8]) / 24.0));
            //write scaled metrics
            for(int i = 1; i <= metricMethods.size(); i++)
                toWrite[i+8] = String.format("%.1f", ScalingComputation.scaleMetricValue(metricMethods.get(i).apply(current, week), maximums.get(i)));

            output.writeNext(toWrite);
        }
        output.close();
    }

    public static double computeStatus(UserMetricComputation user, int week) {
        List<Double> difference = new ArrayList<>();
        difference.add(0.0);
        double scaled_value;

        for(int i = 1; i <= metricMethods.size(); i++) {
            scaled_value = ScalingComputation.scaleMetricValue(metricMethods.get(i).apply(user, week), maximums.get(i));
            difference.add(i, scaled_value - thresholds.get(i));

        }
            /*if(Math.abs(diff) <= 0.5)
                difference.add(i, 0.0);
            else if (diff > 0)
                difference.add(i, diff - 0.5);
            else
                difference.add(i, diff + 0.5);*/

        //System.out.print("User: " + user.getId() + " -- ");
        return weightedSum(difference);
    }

    private static double weightedSum(List<Double> difference) {
        double sum = difference.stream().map(d -> Math.abs(d)).reduce(Double::sum).get().doubleValue();  //pure sum
        //System.out.println(sum);
        double weightedSum = 0;

        for (int i = 1; i < difference.size(); i++) {
            weightedSum += (10 - difference.get(i)) * difference.get(i);
        }

        return weightedSum / (60 - sum);
    }
    
    
    //// TODO: 7/14/2016 remove after 

    private static void initializeST(){
        users = new HashMap<>();
        graduates = new ArrayList<>();
        problems = new HashMap<>();
        maximums = new HashMap<>();
        videoLengths = new HashMap<>();
        thresholds = new HashMap<>();

        metricMethods = new HashMap<>();
        metricMethods.put(1, UserMetricComputation::getSessionsPerWeek);
        metricMethods.put(2, UserMetricComputation::getAverageSessionLength);
        metricMethods.put(3, UserMetricComputation::getAverageTimeBetweenSessions);
        metricMethods.put(4, UserMetricComputation::getForumSessions);
        metricMethods.put(5, UserMetricComputation::getQuizAttempted);
        metricMethods.put(6, UserMetricComputation::getTimeliness);
        metricMethods.put(7, UserMetricComputation::getSessions);
    }
    
    public static void computeMetricsST(String course, int week, String path) throws IOException, ParseException {
        String dataPath = path + "\\data\\";
        String metricsPath = path + "\\metrics\\";
        Edition edition = Edition.CURRENT;

        System.out.println("Initializing course for week " + week + " ... " + course.toUpperCase());
        initializeST();

        System.out.println("Loading data for week " + week + "... " + course.toUpperCase());

        readUsers(users, dataPath + "learners.csv");
        readProblems(dataPath + "problems.csv");

        loadData(edition, dataPath);

        System.out.println("Writing metrics for week " + week + "... " + course.toUpperCase());
        writeMetrics(users, week, metricsPath, metricsPath + "\\" + course.toUpperCase() + "_metrics.csv");

    }
    
    
    
}

class AverageGraduateComputation {
    public static List<Integer> listMetricValues(HashMap<String, UserMetricComputation> users, BiFunction<UserMetricComputation, Integer, Integer> method, int week){

        return users.entrySet().stream()
                .map(e -> method.apply(e.getValue(), week))
                .collect(Collectors.toList());

    }

    public static double getAverage(HashMap<String, UserMetricComputation> users, BiFunction<UserMetricComputation, Integer, Integer> method, int week, int cutOffPercent) {
        List<Integer> allMetricValues = listMetricValues(users, method, week);

        double average;

        if(getCutOffRange(allMetricValues, cutOffPercent).size() == 0)
            return 0;

        average = Math.round(getCutOffRange(allMetricValues, cutOffPercent)
                .stream()
                .mapToInt(e -> e)
                .average()
                .getAsDouble());

        if(Double.isNaN(average))
            return 0;

        return average;
    }

    private static int getMaximum(List<Integer> integers) {
        if (integers.size() == 0)
            return 0;
        return integers.stream().max(Comparator.naturalOrder()).get();
    }

    private static int getMinimum(List<Integer> integers) {
        if (integers.size() == 0)
            return 0;
        return integers.stream().min(Comparator.naturalOrder()).get();
    }

    public static List<Integer> getCutOffRange(List<Integer> values, int cutOffPercent) {
        int min, max, cutOffMin, cutOffMax;

        min = getMinimum(values);
        max = getMaximum(values);

        cutOffMin = min + (max - min) * cutOffPercent / 100;
        cutOffMax = max - (max - min) * cutOffPercent / 100;

        return values.stream()
                .filter(e -> e >= cutOffMin)
                .filter(e -> e <= cutOffMax)
                .collect(Collectors.toList());
    }

    public static int getMaximumInCutOffRange(HashMap<String, UserMetricComputation> users,
                                 BiFunction<UserMetricComputation, Integer, Integer> method, int week, int cutOffPercent) {

        List<Integer> metricValues = listMetricValues(users, method, week);
        return getMaximum(getCutOffRange(metricValues, cutOffPercent));
    }
}

class ScalingComputation {

    public static double getScaledThresholdValue(HashMap<String, UserMetricComputation> users, BiFunction<UserMetricComputation, Integer, Integer> method, int week, int cutOffPercent) {

        double average = AverageGraduateComputation.getAverage(users, method, week, cutOffPercent);
        double max = AverageGraduateComputation.getMaximumInCutOffRange(users, method, week, cutOffPercent);

        if(max == 0)
            return 0;

        return average * 10 / max;
    }

    public static double scaleMetricValue(int value, int maximum) {
        if(maximum == 0)
            return 0;

        if(value > maximum)
            return 10;

        return value * 10.0 / maximum;
    }

}
