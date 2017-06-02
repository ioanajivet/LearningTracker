package precalc;
 
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.crypto.Data;
import java.sql.*;

public class PreCalcServlet extends HttpServlet {

	private DatabaseData edXDatabase = new DatabaseData("com.mysql.jdbc.Driver", "...", "...", "..."); //TODO: update
	private DatabaseData localDatabase = new DatabaseData("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/learning_tracker", "root", ""); //TODO: updat
	private Connection edXConnection = null;
	private Connection localConnection = null;

	private int course_weeks; 	//todo: define the number of weeks in the course

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response) 
		throws IOException, ServletException, SQLException {

	   PrintWriter writer = response.getWriter();
		String reply = "empty reply";
		String userId = "";
	   double[] individual, referenceFrameThisWeek, referenceFrameNextWeek, maximumsThisWeek, maximumsNextWeek;

	   Connection edXConnection = null;
	   Connection localConnection = null;

	   try{

			//1. Extract parameters from the HTTP Request
			String anonId = request.getParameter("userId").toString();
			String courseId = request.getParameter("courseId");
			//todo: add the course_id as a parameter in the script

			//2. Calculate the current week - needed for calculating averages per week and retrieving the reference frames
			int week = 1; //todo: calculate the current week based on a timestamp

			//3. Connect to the edX database and to the local database
		    edXConnection = connectToDatabase(edXDatabase);
			localConnection = connectToDatabase(localDatabase);

			//4. Calculate metrics for the current user
			individual = new double[6];
			individual[0] = getAverageTimePerWeek(edXConnection, courseId, userId);
			individual[1] = getLecturesRevisited(edXConnection, courseId, userId);
			individual[2] = getForumActivity(edXConnection, courseId, userId);
			individual[3] = getQuizAttempted(edXConnection, courseId, userId);
			individual[4] = getProportionTimeOnQuiz(edXConnection, courseId, userId);
			individual[5] = getTimeliness(edXConnection, courseId, userId);

			//5. Get needed data from the local database: the data series for "frame of reference" and the maximum values for scaling
			referenceFrameThisWeek = getDataSeriesFromLocalDatabase(localConnection, week, "precalc_2017_social");
			referenceFrameNextWeek = getDataSeriesFromLocalDatabase(localConnection, week+1, "precalc_2017_social");
			maximumsThisWeek = getDataSeriesFromLocalDatabase(localConnection, week, "precalc_2017_maximum");
			maximumsNextWeek = getDataSeriesFromLocalDatabase(localConnection, week+1, "precalc_2017_maximum");

			//6. Scale the metric values in the range [0-10] so they can be dislayed on the widget
			// The scaling should be done with the maximum value among the previous graduates (for the social group) and the value


			//7. Generate the Learning Tracker script
	   		reply = generateScript(userId, individual, referenceFrameThisWeek, referenceFrameNextWeek, maximumsThisWeek, maximumsNextWeek);

 			//8. Respond to the HTTP request with the generated script
           writer.write(reply);
           
           }
       catch(Exception ex) {
		   ex.getStackTrace();
       } finally {
			writer.close();  // Always close the output writer
		   if(edXConnection != null)
		   		edXConnection.close();
		   if(localConnection != null)
		   		localConnection.close();
       }
   }


	//=========================================
	//====== Metric computation methods =======
	//=========================================

	// Metric 1: AverageTimePerWeek: average time spent on the platform each week
	// Unit: minutes
	// Calculation: summing up the duration of each session that happened before the current week
	// Tables used: "sessions" with the fields:
	//		"course_run_id" - to identify the course
	// 		"learner_id" - to identify sessions belonging to the current user
	// 		"duration" - to calculate the time spent per week

   private int getAverageTimePerWeek(Connection conn, String courseId, String userId) throws SQLException {
	   String query = "SELECT SUM(duration) AS duration FROM sessions WHERE course_run_id='" + courseId + "' AND learner_id='" + userId + "';";

	   Statement st = conn.createStatement();
	   ResultSet res = st.executeQuery(query);

	   int week = 1; 	//todo: identify the number of weeks since the start of the course
	   int total_duration = 0;	//this fields is in seconds in the database

	   if (res.next())
		   total_duration = res.getInt("duration");

	   return (total_duration/60)/week;			//division by 60 to transform into minutes

   }

	// Metric 2: LecturesRevisited: number of videos that have been visited more than once and viewed more than 80% of their total duration
	// Unit: -
	// Calculation:
	// Tables used:

	private int getLecturesRevisited(Connection edXConnection, String courseId, String userId) throws SQLException {
		return 0;
	}

	// Metric 3: ForumActivity: number of contributions to the forum
	// Unit: -
	// Calculation:
	// Tables used:

	private int getForumActivity(Connection edXConnection, String courseId, String userId) throws SQLException {
		return 0;
	}

	// Metric 4: QuizAttempted: number of unique quiz questions that were attempted by a learner
	// Unit: -
	// Calculation:
	// Tables used:

	private int getQuizAttempted(Connection edXConnection, String courseId, String userId) {
		return 0;
	}

	// Metric 5: ProportionTimeOnQuiz: the proportion of time spent on quiz pages from the total time spent on the platform
	// Unit: -
	// Calculation:
	// Tables used:

	private int getProportionTimeOnQuiz(Connection edXConnection, String courseId, String userId) {
		return 0;
	}

	// Metric 6: Timeliness: how early before the deadline learners submit their quiz question answers
	// Unit: hours
	// Calculation:
	// Tables used:

	private int getTimeliness(Connection edXConnection, String courseId, String userId) {
		return 0;
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



	//===============================
	//====== Auxiliary methods ======
	//===============================

	private Connection connectToDatabase(DatabaseData databaseData) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		Connection connection = null;
		Class.forName(databaseData.dbDriver).newInstance();
		connection = DriverManager.getConnection(databaseData.dbURL, databaseData.dbUser, databaseData.dbPass);

		return connection;
	}

	private double[] getDataSeriesFromLocalDatabase(Connection localConnection, int week, String table) throws SQLException{
		double[] profile = new double[6];


		if(week >= course_weeks)	//during the last week of the course, the "next_week" data is the same as the last week
			week = course_weeks;


		String query = "SELECT * FROM " + table + " WHERE week='" + week + "';";

		Statement st = localConnection.createStatement();
		ResultSet res = st.executeQuery(query);

		if (res.next()) {
			for (int i = 1; i <= 6; i++)
				profile[i - 1] = res.getInt("metric_" + i);
		}


		return profile;
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

	private class DatabaseData {
		private String dbURL;
		private String dbUser;
		private String dbPass;
		private String dbDriver;

		public DatabaseData(String dbDriver, String dbURL, String dbUser, String dbPass) {
			this.dbDriver = dbDriver;
			this.dbURL = dbURL;
			this.dbUser = dbUser;
			this.dbPass = dbPass;
		}

		public String getDbDriver() {
			return dbDriver;
		}

		public String getDbURL() {
			return dbURL;
		}

		public String getDbUser() {
			return dbUser;
		}

		public String getDbPass() {
			return dbPass;
		}
	}

}