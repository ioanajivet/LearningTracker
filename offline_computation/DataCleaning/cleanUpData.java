package DataCleaning;

//select for analysis only the active users from both groups
//Active - >5 mins on the platform

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ioana on 5/25/2016.
 */
public class cleanUpData {


    public static void main(String[] args) throws IOException {

        //int week = 8;
        //cleanST(week);

        //cleanPrecalc2015();

        //convert minutes to second for video length
        //videoLength("data\\precalc\\2015\\videos.csv", "data\\precalc\\2015\\videos_s.csv", 3);
        //extractField("data\\precalc\\problems.csv", "data\\precalc\\problem.csv", 0,2, "@");

        //printAnonymizedIds("data\\precalc\\learners_2.csv", "data\\precalc\\anonIds.csv", "data\\precalc\\learn_anon.csv");

        extractField("data\\precalc\\2015\\graduates.csv", "data\\precalc\\2015\\grads.csv", 0,1, "_");
    }

    private static void printAnonymizedIds(String learners, String anon_ids, String result) throws IOException {
        HashMap<String, String> listOfLearners = new HashMap<>();

        CSVReader csvLearners = new CSVReader(new FileReader(learners));
        CSVReader csvAnon = new CSVReader(new FileReader(anon_ids));
        CSVWriter csvWriter = new CSVWriter(new FileWriter(result));

        String [] nextLine, toWrite = new String[2];

        nextLine = csvLearners.readNext();
        while ((nextLine = csvLearners.readNext()) != null)
            listOfLearners.put(nextLine[0], "");
        csvLearners.close();

        nextLine = csvAnon.readNext();
        while ((nextLine = csvAnon.readNext()) != null)
            if(listOfLearners.containsKey(nextLine[0]))
                listOfLearners.put(nextLine[0], nextLine[1]);
        csvAnon.close();

        toWrite = "id#anon_id".split("#");
        csvWriter.writeNext(toWrite);

        for (Map.Entry<String,String> entry : listOfLearners.entrySet()) {
            toWrite[0] = entry.getKey();
            toWrite[1] = entry.getValue();
            csvWriter.writeNext(toWrite);
        }

        csvWriter.close();

    }

    private static void videoLength(String input, String output, int field) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(input));
        CSVWriter csvWriter = new CSVWriter(new FileWriter(output));
        String [] nextLine, toWrite = new String[2];

        nextLine = csvReader.readNext();
        toWrite = "video_id#length".split("#");
        csvWriter.writeNext(toWrite);

        while ((nextLine = csvReader.readNext()) != null) {
            toWrite[0] = nextLine[0];

            toWrite[1] = String.valueOf(Integer.parseInt(nextLine[field].split(":")[0]) * 60
                    + Integer.parseInt(nextLine[field].split(":")[1]));

            csvWriter.writeNext(toWrite);
        }

        csvReader.close();
        csvWriter.close();
    }

    private static void cleanST(int week) throws IOException{
        //sessions.csv
        addField("data\\old_MOOC\\st\\week" + week + "\\data\\sessions.csv", "data\\old_MOOC\\st\\week" + week + "\\data\\sessions_s.csv", 0, 3);
        addField("data\\old_MOOC\\st\\week" + week + "\\data\\sessions_s.csv", "data\\old_MOOC\\st\\week" + week + "\\data\\sessions_ss.csv", 0, 4);
        extractField("data\\old_MOOC\\st\\week" + week + "\\data\\sessions_ss.csv", "data\\old_MOOC\\st\\week" + week + "\\data\\sessions.csv", 1, 1, "_");

        //quiz_session.csv
        extractField("data\\old_MOOC\\st\\week" + week + "\\data\\quiz_sessions.csv", "data\\old_MOOC\\st\\week" + week + "\\data\\quiz_sessions_s.csv", 1, 1, "_");

        //forum_session.csv
        extractField("data\\old_MOOC\\st\\week" + week + "\\data\\forum_sessions.csv", "data\\old_MOOC\\st\\week" + week + "\\data\\forum_sessions_s.csv", 1, 1, "_");

        //observation.csv
        extractField("data\\old_MOOC\\st\\week" + week + "\\data\\observations.csv", "data\\old_MOOC\\st\\week" + week + "\\data\\observations_s.csv", 1, 1, "_");

        //submissions.csv
        extractField("data\\old_MOOC\\st\\week" + week + "\\data\\submissions.csv", "data\\old_MOOC\\st\\week" + week + "\\data\\submissions_s.csv", 1, 1, "_");
        filterField("data\\old_MOOC\\st\\week" + week + "\\data\\submissions_s.csv", "data\\old_MOOC\\st\\week" + week + "\\data\\submissions.csv", 3, "problem_graded");
    }

    private static void cleanPrecalc2015() throws IOException {
        extractField("data\\precalc\\2015\\graduates.csv", "data\\precalc\\2015\\graduates_new.csv", 0, 1, "_");

        //quiz_session.csv
        extractField("data\\precalc\\2015\\quiz_sessions.csv", "data\\precalc\\2015\\quiz_sessions_s.csv", 1, 1, "_");

        //forum_session.csv
        extractField("data\\precalc\\2015\\forum_sessions.csv", "data\\precalc\\2015\\forum_sessions_s.csv", 1, 1, "_");

        //observation.csv
        extractField("data\\precalc\\2015\\observations.csv", "data\\precalc\\2015\\observations_s.csv", 1, 1, "_");

        //submissions.csv
        extractField("data\\precalc\\2015\\submissions.csv", "data\\precalc\\2015\\submissions_s.csv", 1, 1, "_");
        filterField("data\\precalc\\2015\\submissions_s.csv", "data\\precalc\\2015\\submissions.csv", 3, "problem_graded");

    }

    private static void extractDate(String input, String output) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(input));
        CSVWriter csvWriter = new CSVWriter(new FileWriter(output));
        String [] nextLine, toWrite = new String[4];

        while ((nextLine = csvReader.readNext()) != null) {
            for(int i = 0; i < nextLine.length; i++)
                toWrite[i] = nextLine[i];

            toWrite[nextLine.length] = nextLine[0].split("_")[3];

            csvWriter.writeNext(toWrite);
        }

        csvReader.close();
        csvWriter.close();
    }

    private static void addField(String input, String output, int csv_field, int string_field) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(input));
        CSVWriter csvWriter = new CSVWriter(new FileWriter(output));
        String [] nextLine, toWrite;

        nextLine = csvReader.readNext();
        toWrite = new String[nextLine.length + 1];

        for(int i = 0; i < nextLine.length; i++)
            toWrite[i] = nextLine[i];
        csvWriter.writeNext(toWrite);

        while ((nextLine = csvReader.readNext()) != null) {
            for(int i = 0; i < nextLine.length; i++)
                toWrite[i] = nextLine[i];

            toWrite[nextLine.length] = nextLine[csv_field].split("_")[string_field];
            csvWriter.writeNext(toWrite);
        }

        csvReader.close();
        csvWriter.close();
    }

    //input file, output file, the field in the CSV files that needs to be extracted, the field (separated by '_') in the string that needs to be extracted
    private static void extractField(String input, String output, int csv_field, int string_field, String regEx) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(input));
        CSVWriter csvWriter = new CSVWriter(new FileWriter(output));
        String [] nextLine;

        nextLine = csvReader.readNext();
        csvWriter.writeNext(nextLine);

        while ((nextLine = csvReader.readNext()) != null) {
            nextLine[csv_field] = nextLine[csv_field].split(regEx)[string_field];
            csvWriter.writeNext(nextLine);
        }

        csvReader.close();
        csvWriter.close();

    }

    private static void filterField(String input, String output, int csv_field, String field_value) throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(input));
        CSVWriter csvWriter = new CSVWriter(new FileWriter(output));
        String [] nextLine;

        nextLine = csvReader.readNext();
        csvWriter.writeNext(nextLine);

        while ((nextLine = csvReader.readNext()) != null) {
            if(nextLine[csv_field].compareTo(field_value) != 0)
                continue;

            csvWriter.writeNext(nextLine);
        }

        csvReader.close();
        csvWriter.close();

    }

}
