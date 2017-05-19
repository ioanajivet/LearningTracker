import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ioana on 6/8/2016.
 */
public class Utils {

    public static int getWeek(MetricComputation.Edition edition, String startTime) {
        if(edition == MetricComputation.Edition.PREVIOUS)
            return getWeekPrevious(startTime);
        return getWeekCurrent(startTime);
    }

    //This is for ST
    /*private static int getWeekPrevious(String startTime) {
        if(startTime.compareTo("2015-01-27") > 0 && startTime.compareTo("2015-02-03") < 0)
            return 1;
        if(startTime.compareTo("2015-02-03") > 0 && startTime.compareTo("2015-02-10") < 0)
            return 2;
        if(startTime.compareTo("2015-02-10") > 0 && startTime.compareTo("2015-02-17") < 0)
            return 3;
        if(startTime.compareTo("2015-02-17") > 0 && startTime.compareTo("2015-02-24") < 0)
            return 4;
        if(startTime.compareTo("2015-02-24") > 0 && startTime.compareTo("2015-03-03") < 0)
            return 5;
        if(startTime.compareTo("2015-03-03") > 0 && startTime.compareTo("2015-03-10") < 0)
            return 6;
        if(startTime.compareTo("2015-03-10") > 0 && startTime.compareTo("2015-03-17") < 0)
            return 7;
        if(startTime.compareTo("2015-03-17") > 0 && startTime.compareTo("2015-03-24") < 0)
            return 8;
        if(startTime.compareTo("2015-03-24") > 0 && startTime.compareTo("2015-03-31") < 0)
            return 9;
        if(startTime.compareTo("2015-03-31") > 0 && startTime.compareTo("2015-04-08") < 0)
            return 10;
        return 99;
    }*/

    private static int getWeekPrevious(String startTime) {
        if(startTime.compareTo("2015-07-07") > 0 && startTime.compareTo("2015-07-14") < 0)
            return 1;
        if(startTime.compareTo("2015-07-14") > 0 && startTime.compareTo("2015-07-21") < 0)
            return 2;
        if(startTime.compareTo("2015-07-21") > 0 && startTime.compareTo("2015-07-28") < 0)
            return 3;
        if(startTime.compareTo("2015-07-28") > 0 && startTime.compareTo("2015-08-04") < 0)
            return 4;
        if(startTime.compareTo("2015-08-04") > 0 && startTime.compareTo("2015-08-11") < 0)
            return 5;
        if(startTime.compareTo("2015-08-11") > 0 && startTime.compareTo("2015-08-18") < 0)
            return 6;
        if(startTime.compareTo("2015-08-18") > 0 && startTime.compareTo("2015-08-25") < 0)
            return 7;
        if(startTime.compareTo("2015-08-25") > 0 && startTime.compareTo("2015-09-01") < 0)
            return 8;
        if(startTime.compareTo("2015-09-01") > 0 && startTime.compareTo("2015-09-08") < 0)
            return 9;
        if(startTime.compareTo("2015-09-08") > 0 && startTime.compareTo("2015-09-15") < 0)
            return 10;
        if(startTime.compareTo("2015-09-15") > 0 && startTime.compareTo("2015-09-22") < 0)
            return 11;
        if(startTime.compareTo("2015-09-22") > 0 && startTime.compareTo("2015-09-30") < 0)
            return 12;
        return 99;
    }

    //todo: update with the current weeks
    //this is for ST
    private static int getWeekCurrentST(String startTime) {
        if(startTime.compareTo("2016-04-12") > 0 && startTime.compareTo("2016-04-19") < 0)
            return 1;
        if(startTime.compareTo("2016-04-19") > 0 && startTime.compareTo("2016-04-26") < 0)
            return 2;
        if(startTime.compareTo("2016-04-26") > 0 && startTime.compareTo("2016-05-03") < 0)
            return 3;
        if(startTime.compareTo("2016-05-03") > 0 && startTime.compareTo("2016-05-10") < 0)
            return 4;
        if(startTime.compareTo("2016-05-10") > 0 && startTime.compareTo("2016-05-17") < 0)
            return 5;
        if(startTime.compareTo("2016-05-17") > 0 && startTime.compareTo("2016-05-24") < 0)
            return 6;
        if(startTime.compareTo("2016-05-24") > 0 && startTime.compareTo("2016-05-31") < 0)
            return 7;
        if(startTime.compareTo("2016-05-31") > 0 && startTime.compareTo("2016-06-07") < 0)
            return 8;
        if(startTime.compareTo("2016-06-07") > 0 && startTime.compareTo("2016-06-14") < 0)
            return 9;
        if(startTime.compareTo("2016-06-14") > 0 && startTime.compareTo("2016-06-20") < 0)
            return 10;
        return 99;
    }

    private static int getWeekCurrent(String startTime) {
        if(startTime.compareTo("2016-06-21") > 0 && startTime.compareTo("2016-06-28") < 0)
            return 1;
        if(startTime.compareTo("2016-06-28") > 0 && startTime.compareTo("2016-07-05") < 0)
            return 2;
        if(startTime.compareTo("2016-07-05") > 0 && startTime.compareTo("2016-07-12") < 0)
            return 3;
        if(startTime.compareTo("2016-07-12") > 0 && startTime.compareTo("2016-07-19") < 0)
            return 4;
        if(startTime.compareTo("2016-07-19") > 0 && startTime.compareTo("2016-07-26") < 0)
            return 5;
        if(startTime.compareTo("2016-07-26") > 0 && startTime.compareTo("2016-08-02") < 0)
            return 6;
        if(startTime.compareTo("2016-08-02") > 0 && startTime.compareTo("2016-08-09") < 0)
            return 7;
        if(startTime.compareTo("2016-08-09") > 0 && startTime.compareTo("2016-08-16") < 0)
            return 8;
        if(startTime.compareTo("2016-08-16") > 0 && startTime.compareTo("2016-08-23") < 0)
            return 9;
        if(startTime.compareTo("2016-08-23") > 0 && startTime.compareTo("2016-08-30") < 0)
            return 10;
        if(startTime.compareTo("2016-08-30") > 0 && startTime.compareTo("2016-09-06") < 0)
            return 11;
        if(startTime.compareTo("2016-09-06") > 0 && startTime.compareTo("2016-09-13") < 0)
            return 12;
        if(startTime.compareTo("2016-09-13") > 0 && startTime.compareTo("2016-09-20") < 0)
            return 13;
        if(startTime.compareTo("2016-09-20") > 0 && startTime.compareTo("2016-09-30") < 0)
            return 14;
        return 99;
    }

    //FOR ST
/*    public static String getProblemDeadline(MetricComputation.Edition edition) {
        if(edition == MetricComputation.Edition.CURRENT)
            return "2015-04-08 12:00:00";
        //todo: update to actual date if wee change the course we use!!!
        return "2015-04-08 12:00:00";
    }*/
    //FOR PreCalc
    public static String getProblemDeadline(MetricComputation.Edition edition) {
        if(edition == MetricComputation.Edition.CURRENT)
            return "2016-09-29 23:30:00";
        //todo: update to actual date if wee change the course we use!!!
        return "2015-09-29 23:30:00";
    }

    public static void checkForDirectory(String filepath) {
        File theDir = new File(filepath);

        // if the directory does not exist, create it
        if (!theDir.exists()) {
            System.out.println("Creating directory: " + filepath);
            boolean result = false;

            try{
                theDir.mkdir();
                result = true;
            }
            catch(SecurityException se){
                System.out.println(se);
            }
            if(result) {
                System.out.println("Directory created: " + filepath);
            }
        }
    }

    public static String formatTimestamp(String timestamp) {
        //2015-01-29T14:44:39.721793+00:00
        String date = timestamp.split("\\.")[0];
        return date.replace("T", " ");
    }

    private static Date getDateFromString(String dateString) throws ParseException {
        //input date: "2014-11-11 12:00:00"
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return format.parse(dateString);
        }
        catch (ParseException e) {
            System.out.println("Invalid date");
            throw e;
        }
    }

    public static long differenceBetweenDatesInHours(String deadline, String submission){
        long diff = 0;
        try {
            diff = getDateFromString(deadline).getTime() - getDateFromString(submission).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if(diff > 0)
            return TimeUnit.MILLISECONDS.toHours(diff);

        return 0;
    }


}


