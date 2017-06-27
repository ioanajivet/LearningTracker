import precalc_2017.DatabaseData;
import precalc_2017.SQLDataAccessObject;

import java.io.*;
import java.sql.*;
import java.util.Comparator;
import java.util.Map;

public class PreCalcReferenceFrame {

	private static DatabaseData edXDatabase = new DatabaseData("com.mysql.jdbc.Driver", "...", "...", "..."); //TODO: update
	private static DatabaseData localDatabase = new DatabaseData("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/learning_tracker?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", "root"); //TODO: update

	private int course_weeks;

	public static void main(String[] args) throws IOException, SQLException {

		//String courseId = args[0];	//the id of the edition of the MOOC
		String courseId = "111";	//the id of the edition of the MOOC
		Map<Integer, String> weekEndDates = null;

	   double[] referenceFrame, maximums;

	   try{

			//1. Calculate the amount of weeks based on the start and end data
			int weeks = 1; //todo: calculate the amount of weeks

		   //2. Connect to the edX database and to the local database
		   //edXConnection = connectToDatabase(edXDatabase);
		   //precalc_2017.SQLDataAccessObject edXDBObject = new precalc_2017.SQLDataAccessObject(edXDatabase);
		   SQLDataAccessObject localDBObject = new SQLDataAccessObject(localDatabase);

		   //3. Get the start and end date of the MOOC and calculate the number of weeks and the end date of each week
		   //todo: put it in a Map as well;

		    //3. for each week, calculate the average for each metric and then write it in the local database
		   for(int i = 1; i <= weeks; i++) {
		       double[] individual = calculateReferenceFrameForWeek(localDBObject, courseId, i, weekEndDates.get(i));
			   //double[] individual = calculateReferenceFrameForWeek(edXDBObject, courseId, i, weekEndDates.get(i));
			   //todo: writeReferenceFrameforWeek(localDBObject, courseId, i);
			   //todo: writeMaximumforWeek(localDBObject, courseId, i);
		   }

           
           }
       catch(Exception ex) {
		   System.out.println("Exception" + ex);
		   ex.getStackTrace();
       } finally {
		   //todo: close database connections - make method in the class
       }
   }

	private static double[] calculateReferenceFrameForWeek(SQLDataAccessObject database, String courseId, int week, String weekEndDate) throws SQLException {
		double[] referenceFrame = new double[6];
		referenceFrame[0] = getAverageTimePerWeek(database, courseId, week, weekEndDate);
		//referenceFrame[1] = getLecturesRevisited(edXConnection, courseId, week);
		referenceFrame[2] = getForumContribution(database, courseId, weekEndDate);
		referenceFrame[3] = getQuizAttempted(database, courseId, weekEndDate);
		//referenceFrame[4] = getProportionTimeOnQuiz(edXConnection, courseId, week);
		//referenceFrame[5] = getTimeliness(edXConnection, courseId, week);

		return referenceFrame;
	}

	//==============================================================
	//====== Metric computation methods for reference frames =======
	//==============================================================

	// Metric 1: AverageTimePerWeek: average time spent on the platform each week
	// Unit: minutes

   private static double getAverageTimePerWeek(SQLDataAccessObject database, String courseId, int week, String weekEndDate) throws SQLException {
	   Map<Integer, Double> averageTimePerWeekByLearner = database.getLearnerTimeOnPlatform(courseId, weekEndDate);

	   for (Map.Entry<Integer, Double> entry: averageTimePerWeekByLearner.entrySet()) {
			averageTimePerWeekByLearner.put(entry.getKey(), Math.round(entry.getValue()/week/60)*1.0);	//division by 60 to transform into minutes
	   }

	   return metricAverage(averageTimePerWeekByLearner);

   }

	// Metric 2: LecturesRevisited: number of videos that have been visited more than once and viewed more than 80% of their total duration
	// Unit: -
	
	private static double getLecturesRevisited(SQLDataAccessObject database, String courseId, String weekEndDate) throws SQLException{
		return 0;

	}

	//Metric 3:
	private static double getForumContribution(SQLDataAccessObject database, String courseId, String weekEndDate) throws SQLException {
		Map<Integer, Double> forumCOntributionByLearner = database.getLearnerForumContributions(courseId, weekEndDate);
		return metricAverage(forumCOntributionByLearner);
	}

	//Metric 4:

	private static double getQuizAttempted(SQLDataAccessObject database, String courseId, String weekEndDate) throws SQLException {
		Map<Integer, Double> quizAttemptedByLearner = database.getLearnerQuizAttempted(courseId, weekEndDate);
		return metricAverage(quizAttemptedByLearner);
	}

	// Metric 5: ProportionTimeOnQuiz: the proportion of time spent on quiz pages from the total time spent on the platform
	// Unit: -

	
	private static double getProportionTimeOnQuiz(SQLDataAccessObject database, String courseId, String weekEndDate) throws SQLException {
		   
		   return 0;
	}

	// Metric 6: Timeliness: how early before the deadline learners submit their quiz question answers
	// Unit: hours
	
	private static double getTimeliness(Connection edXConnection, String courseId, String userId) throws SQLException{

		return 0;

	}


	//===============================
	//====== Auxiliary methods ======
	//===============================

	public static double metricAverage(Map<Integer, Double> metricValues) {
		int cutOffPercent = 5;
		double cutOffMin, cutOffMax, average;

		double max =
				metricValues.entrySet().stream()
						.map(e -> e.getValue())
						.max(Comparator.naturalOrder()).get();

		double min =
				metricValues.entrySet().stream()
						.map(e -> e.getValue())
						.min(Comparator.naturalOrder()).get();

		cutOffMin = min + (max - min) * cutOffPercent / 100;
		cutOffMax = max - (max - min) * cutOffPercent / 100;

		average = metricValues.entrySet().stream()
				.map(e -> e.getValue())
				.filter(e -> e >= cutOffMin)
				.filter(e -> e <= cutOffMax)
				.mapToDouble(e -> e)
				.average()
				.getAsDouble();

		if(Double.isNaN(average))
			return 0;

		return average;

	}

	public static double metricMaximum(Map<Integer, Double> metricValues) {
		int cutOffPercent = 5;
		double cutOffMax;

		double max =
				metricValues.entrySet().stream()
						.map(e -> e.getValue())
						.max(Comparator.naturalOrder())
						.get();

		double min =
				metricValues.entrySet().stream()
						.map(e -> e.getValue())
						.min(Comparator.naturalOrder()).get();

		if(Double.isNaN(min) || Double.isNaN(max))
			return 0;

		return max - (max - min) * cutOffPercent / 100;

	}

}

