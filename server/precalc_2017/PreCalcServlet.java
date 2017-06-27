package precalc_2017;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PreCalcServlet extends HttpServlet {

	private static final int COURSE_WEEKS = 14;					//TODO: UPDATE
	private static final String COURSE_RUN_ID = "43";			//todo: update the course_run_id. Should it remain hardcoded for this case?
	private static final String REFERENCE_FRAME_TABLE = "precalc_2017_social";
	private static final String MAXIMUM_TABLE = "precalc_2017_maximum";

	private DatabaseData edXDatabase = new DatabaseData("com.mysql.jdbc.Driver", "...", "...", "..."); 											//TODO: update
	private DatabaseData localDatabase = new DatabaseData("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/learning_tracker", "root", ""); //TODO: update

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response) 
		throws IOException, ServletException {

	   PrintWriter writer = response.getWriter();
	   String reply = "empty reply";
	   String userId;
	   double[] individual, referenceFrameThisWeek, referenceFrameNextWeek, maximumsThisWeek, maximumsNextWeek;
	   Date timestamp;
	   int week;

	   SQLDataAccessObject edXConnection = null;
	   SQLDataAccessObject localConnection = null;

	   //1. Extract parameters from the HTTP Request
	   userId = request.getParameter("userId");
	   timestamp = new Date(request.getDateHeader("Date"));


	   try {

		   //2. Connect to the edX database and to the local database
		   edXConnection = new SQLDataAccessObject(edXDatabase);
		   localConnection = new SQLDataAccessObject(localDatabase);

		   //3. Calculate the number of weeks in the course
		   //method to calculate the number of weeks based on the start and end dates of the course, but it is not needed if we use this version only once in precalc.
		   //course_weeks = getCourseWeeks(edXConnection, COURSE_RUN_ID);

		   //4. Calculate the current week - needed for calculating averages per week and retrieving the reference frames
		   week = getCurrentWeek(edXConnection, timestamp, COURSE_RUN_ID);

		   //5. Calculate metrics for the current user
		   individual = new double[6];
		   individual[0] = getAverageTimePerWeek(edXConnection, COURSE_RUN_ID, userId, week);
		   individual[1] = getLecturesRevisited(edXConnection, localConnection, COURSE_RUN_ID, userId);
		   individual[2] = getForumActivity(edXConnection, COURSE_RUN_ID, userId);
		   individual[3] = getQuizAttempted(edXConnection, COURSE_RUN_ID, userId);
		   individual[4] = getProportionTimeOnQuiz(edXConnection, COURSE_RUN_ID, userId);
		   individual[5] = getTimeliness(edXConnection, COURSE_RUN_ID, userId);

		   //6. Get needed data from the local database: the data series for "frame of reference" and the maximum values for scaling
		   referenceFrameThisWeek = getDataSeriesFromLocalDatabase(localConnection, week, REFERENCE_FRAME_TABLE);
		   referenceFrameNextWeek = getDataSeriesFromLocalDatabase(localConnection, week + 1, REFERENCE_FRAME_TABLE);
		   maximumsThisWeek = getDataSeriesFromLocalDatabase(localConnection, week, MAXIMUM_TABLE);
		   maximumsNextWeek = getDataSeriesFromLocalDatabase(localConnection, week + 1, MAXIMUM_TABLE);

		   //7. Generate the Learning Tracker script
		   reply = generateScript(userId, individual, referenceFrameThisWeek, referenceFrameNextWeek, maximumsThisWeek, maximumsNextWeek);

		   //8. Respond to the HTTP request with the generated script
		   writer.write(reply);

	   } catch (Exception ex) {
		   ex.getStackTrace();
	   } finally {
		   writer.close();  // Always close the output writer
		   if (edXConnection != null)
			   edXConnection.closeConnectionQuietly();
		   if (localConnection != null)
			   localConnection.closeConnectionQuietly();
	   }

   }

	//=========================================
	//====== Metric computation methods =======
	//=========================================

	// Metric 1: AverageTimePerWeek: average time spent on the platform each week
	// Unit: minutes

   private int getAverageTimePerWeek(SQLDataAccessObject database, String courseRunId, String userId, int week) throws SQLException {
	   int timeOnPlatform = database.getTimeOnPlatform(courseRunId, userId);

	   return timeOnPlatform/60/week; 		//convert the value from seconds to minutes

   }

	// Metric 2: LecturesRevisited: number of videos that have been visited more than once and viewed more than 80% of their total duration
	// Unit: -

	private int getLecturesRevisited(SQLDataAccessObject edXDatabase, SQLDataAccessObject localDatabase, String courseId, String userId) throws SQLException{
		Map<String, Integer> watchedVideosCount = edXDatabase.getWatchedVideosCount(courseId, userId);
		Map<String, Integer> watchedVideosLength = edXDatabase.getWatchedVideosWithDuration(courseId, userId);
		Map<String, Integer> videosLength = localDatabase.getVideosLengths(courseId);

		return (int) watchedVideosCount.entrySet().stream()
				.filter(e -> e.getValue() > 1)
				.filter(e -> watchedVideosLength.get(e.getKey()) >= 0.8 * videosLength.get(e.getKey()))
				.count();

	}

	// Metric 3: ForumActivity: number of contributions to the forum
	// Unit: -

	private int getForumActivity(SQLDataAccessObject database, String courseId, String userId) throws SQLException{

		return database.getForumContributions(courseId, userId);
	}

	// Metric 4: QuizAttempted: number of unique quiz questions that were attempted by a learner
	// Unit: -

	private int getQuizAttempted(SQLDataAccessObject database, String courseId, String userId) throws SQLException {

		return database.getQuizAttempted(courseId, userId);
	}

	// Metric 5: ProportionTimeOnQuiz: the proportion of time spent on quiz pages from the total time spent on the platform
	// Unit: -
	
	private int getProportionTimeOnQuiz(SQLDataAccessObject database, String courseId, String userId) throws SQLException {
		int total_duration = database.getTimeOnPlatform(courseId, userId);
		int quiz_duration = database.getTimeOnQuiz(courseId, userId);

		if(total_duration == 0)
			return 0;

		return quiz_duration * 100 / total_duration;
	}

	// Metric 6: Timeliness: how early before the deadline learners submit their quiz question answers
	// Unit: hours
	private int getTimeliness(SQLDataAccessObject database, String courseId, String userId) throws SQLException{
		return database.getTimeliness(courseId, userId);
	}


	//===============================
	//====== Auxiliary methods ======
	//===============================

	private double[] getDataSeriesFromLocalDatabase(SQLDataAccessObject database, long week, String table) throws SQLException{
		double[] profile = new double[6];

		if(week >= COURSE_WEEKS)	//during the last week of the course, the "next_week" data is the same as the last week
			week = COURSE_WEEKS;

		return database.getDataSeries(week, table);
	}

	private String composeString(double[] values) {
		String result = "[";

		for(int i = 0; i < values.length - 1; i++)
			result += values[i] + ", ";
		result += values[values.length-1] + "]";

		return result;
	}

	private double[] scaleByMaximum(double [] dataSeries, double [] maximum) {
		//scales the dataSeries on a scale from 0 to 10, where 10 is the corresponding value in the maximum series
		double[] scaledValues = new double[6];
		for (int i = 0; i < 6; i++)
			scaledValues[i] = Math.floor(dataSeries[i]/maximum[i] * 10 * 100) / 100;

		return scaledValues;
	}

	public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		long diffInMillies = date2.getTime() - date1.getTime();
		return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
	}

	private int getCurrentWeek(SQLDataAccessObject database, Date timestamp, String courseRunId) throws SQLException, ParseException {
		Date startDate = parseStringToDate(database.getCourseStartDate(courseRunId));
		return (int) getDateDiff(startDate, timestamp, TimeUnit.DAYS) / 7;
	}

	private Date parseStringToDate(String dateString) throws ParseException {
		//input date: "2014-11-11 12:00:00"
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.parse(dateString);
	}

	private int getCourseWeeks(SQLDataAccessObject database, String courseRunId) throws SQLException, ParseException {
		Date startDate = parseStringToDate(database.getCourseStartDate(courseRunId));
		Date endDate = parseStringToDate(database.getCourseEndDate(courseRunId));
		return (int) getDateDiff(startDate, endDate, TimeUnit.DAYS) / 7;
	}


	//===============================
	//====== Script generation ======
	//===============================

	// Division of the treatment and control groups
	// 0 = control group = no widget
	// 1 = Treatment individual = no frame of reference
	// 2 = Treatment social = successful learners
	// 3 = Treatment yardstick = teacher set goals


	private String generateScript(String userId, double[] individual, double[] referenceFrameThisWeek, double[] referenceFrameNextWeek, double[] maximumsThisWeek, double[] maximumsNextWeek) {

		String script = "";

		if(Integer.parseInt(userId) % 4 != 0)			// for the control group (userId modulo 4 == 0), do not show the widget
			script = "$('#wrapper-widget').show();\n";

		script += "\n" +
				"var metricNames = ['Average time per week',\n" +
				"'Revisited video-lectures',\n" +
				"'Forum contributions',\n" +
				"'Quiz questions attempted ',\n" +
				"'Proportion of time spent on quiz questions',\n" +
				"'Timeliness of quiz question submissions'];\n" +
				"var metricUnits = ['h',\n" +
				"'',\n" +
				"'',\n" +
				"'',\n" +
				"'%',\n" +
				"'days'];\n" +
				"\n" +
				"var values = " + individual + ";\n" +
				"var lastWeek = " + composeString(referenceFrameThisWeek) + ";\n" +
				"var thisWeek = " + composeString(referenceFrameNextWeek) + ";\n" +
				"\n" +
				"function getSeriesValue(chart, i) {\n" +
				"\t\n" +
				"\tif(chart.points[i].series.name == 'You')\n" +
				"\t\treturn values[chart.x/60];\n" +
				"\telse if(chart.points[i].series.name == 'Average graduate last week')\n" +
				"\t\treturn lastWeek[chart.x/60];\n" +
				"\telse \n" +
				"\t\treturn thisWeek[chart.x/60];\n" +
				"}\n" +
				"\n" +
				"function loadWidget() {\n" +
				"\t\n" +
				"\t$('#container').highcharts({\n" +
				"\t\tchart: {\n" +
				"\t\t\tmarginTop: 120,\n" +
				"\t\t\tpolar: true,\n" +
				"\t\t\tstyle: {\n" +
				"\t\t\t\tfontFamily: 'Open Sans, sans-serif'\n" +
				"\t\t\t},\n" +
				"\t\t\ttype: 'area'\n" +
				"\t\t},\n" +
				"\n" +
				"\t\ttitle: {\n" +
				"\t\t\ttext: 'Learning tracker',\n" +
				"\t\t\tstyle: {\n" +
				"\t\t\t\talign: 'left'\n" +
				"\t\t\t}\n" +
				"\t\t},\n" +
				"\n" +
				"\t\tcredits: {\n" +
				"\t\t\tenabled: false\n" +
				"\t\t},\n" +
				"\n" +
				"\t\tlegend: {\n" +
				"\t\t\treversed: true\n" +
				"\t\t},\n" +
				"\n" +
				"\t\tpane: {\n" +
				"\t\t\tstartAngle: 0,\n" +
				"\t\t\tendAngle: 360\n" +
				"\t\t},\n" +
				"\n" +
				"\t\txAxis: {\n" +
				"\t\t\ttickInterval: 60,\n" +
				"\t\t\tmin: 0,\n" +
				"\t\t\tmax: 360,\n" +
				"\t\t\tlabels: {\n" +
				"\t\t\t\tformatter: function () {\n" +
				"\t\t\t\t\treturn metricNames[this.value/60];\n" +
				"\t\t\t\t}\n" +
				"\t\t\t},\n" +
				"\t\t\tgridLineWidth: 1\n" +
				"\t\t},\n" +
				"\n" +
				"\t\tyAxis: {\n" +
				"\t\t\tmin: 0,\n" +
				"\t\t\tmax: 10,\n" +
				"\t\t\tgridLineWidth: 1,\n" +
				"\t\t\tlabels: {\n" +
				"\t\t\t\tenabled: false\n" +
				"\t\t\t},\n" +
				"\t\t\ttickPositions: [0, 5, 10],\n" +
				"\t\t\tvisible: true\n" +
				"\t\t},\n" +
				"\n" +
				"\t\tplotOptions: {\n" +
				"\t\t\tseries: {\n" +
				"\t\t\t\tallowPointSelect: true,\n" +
				"\t\t\t\tpointStart: 0,\n" +
				"\t\t\t\tpointInterval: 60,\n" +
				"\t\t\t\tcursor: 'pointer',\n" +
				"\t\t\t\tmarker: {\n" +
				"\t\t\t\t\tsymbol: 'diamond',\n" +
				"\t\t\t\t\tradius: 3\n" +
				"\t\t\t\t}\n" +
				"\t\t\t},\n" +
				"\t\t\tcolumn: {\n" +
				"\t\t\t\tpointPadding: 0,\n" +
				"\t\t\t\tgroupPadding: 0\n" +
				"\t\t\t}\n" +
				"\t\t},\n" +
				"\n" +
				"\t\ttooltip: {\n" +
				"\t\t\tshared: true,\n" +
				"\t\t\tformatter: function () {\n" +
				"\t\t\t\tvar tooltip_text = '<b>' + metricNames[this.x/60] + '</b>';\n" +
				"\t\t\t\tvar unit = metricUnits[this.x/60];\n" +
				"\n" +
				"\t\t\t\tfor (i = this.points.length - 1; i >= 0; i--) { \n" +
				"\t\t\t\t\ttooltip_text += '<br/>' + this.points[i].series.name + ': <b>' + getSeriesValue(this, i) + ' ' + unit + '</b>';\n" +
				"\t\t\t\t}\n" +
				"\n" +
				"\t\t\t\treturn tooltip_text;\n" +
				"\t\t\t},\n" +
				"\t\t},\n" +
				"\t\t\n" +
				"\t\tseries: [\t\t\n" +

				getScriptReferenceFrameDataSeries(userId,
						scaleByMaximum(referenceFrameThisWeek, maximumsThisWeek),
						scaleByMaximum(referenceFrameNextWeek, maximumsNextWeek)) +

				"\n" +
				"\t\t{\n" +
				"\t\t\tname: 'You',\n" +
				"\t\t\tcolor: 'rgba(144, 202, 249, 0.5)',\n" +
				"\t\t\tdata: " + composeString(scaleByMaximum(individual, maximumsThisWeek)) + "\n" +
				"\t\t}]\n" +
				"\t});\n" +
				"}\n" +
				"\t\t\n" +
				"loadWidget();";

		return script;
	}

	private String getScriptReferenceFrameDataSeries(String userId, double[] referenceFrameThisWeekScaled, double[] referenceFrameNextWeekScaled) {
		String legendReferenceFrame = "";

		//customize the name in the legend based on the treatment group
		if(Integer.parseInt(userId) % 4 == 2)
			legendReferenceFrame = "Average graduate";

		if(Integer.parseInt(userId) % 4 == 3)
			legendReferenceFrame = "Teacher recommendation for";

		return "\t\t{\n" +
				"\t\t\ttype: 'line',\n" +
				"\t\t\tname: '" + legendReferenceFrame + " next week',\n" +
				"\t\t\tcolor: 'rgba(188, 64, 119, 0.5)',\n" +
				"\t\t\tdata: " + composeString(referenceFrameNextWeekScaled) + ",\n" +
				"\t\t\tvisible: false\n" +
				"\t\t},\n" +
				"\n" +
				"\t\t{\n" +
				"\t\t\tname: '" + legendReferenceFrame + " this week',\n" +
				"\t\t\tcolor: 'rgba(255, 255, 102, 0.5)',\n" +
				"\t\t\tdata: " + composeString(referenceFrameThisWeekScaled) + "\n" +
				"\n" +
				"\t\t},\n";
	}

}
