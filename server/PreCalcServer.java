package precalc;
 
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.crypto.Data;
import java.sql.*;

public class PreCalcServlet extends HttpServlet {

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response) 
		throws IOException, ServletException {

	   PrintWriter writer = response.getWriter();
		String reply = "empty reply";
		String userId = "",
			individual = "",
			individualScaled = "",
			referenceFrameThisWeek = "",
			referenceFrameThisWeekScaled = "",
			referenceFrameNextWeek = "",
			referenceFrameNextWeekScaled = "";

        try{

			//1. Extract parameters from the HTTP Request
			String anonId = request.getParameter("anonId").toString();
			String courseId = request.getParameter("course");
			//todo: add the course_id as a parameter in the script

			//2. Connect to the edX database
			DatabaseData edXDatabase = new DatabaseData("com.mysql.jdbc.Driver", "...", "...", "..."); //TODO: update
		    Connection edXConnection = connectToDatabase(edXDatabase);

			//3. Connect to the local database
			DatabaseData localDatabase = new DatabaseData("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/learning_tracker", "root", ""); //TODO: update
			Connection localConnection = connectToDatabase(localDatabase);

			//3. Get the short user ID instead of the anonymous ID
			userId = getUserId(edXConnection, anonId);
			//todo: based on the database structure, this might be changed
			//todo: check if it is possible to get it from the database - otherwise we need to send HTTP requests directly with the short user id (get it through "analytics" and not through %%USER_ID%%


			//4. Calculate metrics for the current user in the requested week
			int avgTimePerWeek = getAverageTimePerWeek(edXConnection, courseId, userId);
			int lecturesRevisited = getLecturesRevisited(edXConnection, courseId, userId);
			int forumActivity = getForumActivity(edXConnection, courseId, userId);
			int quizAttempted = getQuizAttempted(edXConnection, courseId, userId);
			int proportionTimeOnQuiz = getProportionTimeOnQuiz(edXConnection, courseId, userId);
			int timeliness = getTimeliness(edXConnection, courseId, userId);

			//todo: concatenate the values into the "individual" variable to integrate it in the script
			individual = "";

			//5. Scale the metric values in the range [0-10] so they can be dislayed on the widget
			// The scaling should be done with the maximum value among the previous graduates (for the social group) and the value
			//todo

			//todo: concatenate the values into the "individualScaled" variable to integrate it in the script

			//6. Get the "frame of reference" from the local database based on the user ID (division in 4 groups based on the value of user_id modulo 4)
			// 0 = control group = no widget
			// 1 = individual = no frame of reference
			// 2 = social = successful learners
			// 3 = teacher set goals
			referenceFrameThisWeek = "";
			referenceFrameThisWeekScaled = "";
			referenceFrameNextWeek = "";
			referenceFrameNextWeekScaled = "";
			//todo

			//7. Generate the Learning Tracker script
	   		reply = generateScript(userId, individual, individualScaled, referenceFrameThisWeek, referenceFrameThisWeekScaled, referenceFrameNextWeek, referenceFrameNextWeekScaled);
			//todo: customize the script based on the treatment group in which the learners are

 			//8. Respond to the request with the generated script
           writer.write(reply);
           
           }
       catch(Exception ex) {
		   ex.getStackTrace();
       } finally {
			writer.close();  // Always close the output writer
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
	
	private int getLecturesRevisited(Connection edXConnection, String courseId, String userId) {
		return 0;
	}

	// Metric 3: ForumActivity: number of contributions to the forum
	// Unit: -
	// Calculation: summing up forum contribution based on each user's posts 
	// Tables used: "forum_interaction" with the fields:
    //		"course_run_id" - to identify the course
    // 		"learner_id" - to identify the current user
    // 		"post_id" - to calculate the number of user's forum posts

	private int getForumActivity(Connection edXConnection, String courseId, String userId) throws SQLException{
		String query = "SELECT COUNT(post_id) AS post_count FROM forum_interaction WHERE course_run_id='" + courseId + "' AND learner_id='" + userId + "';";
				
		Statement stmt = edXConnection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		
		int forum_contribution = 0;
		
		if (rs.next()){
			forum_contribution = rs.getInt("post_count");
		}

		return forum_contribution;
	}

	// Metric 4: QuizAttempted: number of unique quiz questions that were attempted by a learner
	// Unit: -
	// Calculation: summing the number of unique quiz attempts by a learner 
	// Tables used: "submissions" with the fields of:
	//		"course_run_id" - to identify the course
	// 		"learner_id" - to identify the current user
	// 		"question_id" - to calculate the number of user's quiz attempts

	private int getQuizAttempted(Connection edXConnection, String courseId, String userId) throws SQLException {
		String query = "SELECT COUNT(DISTINCT question_id) AS quiz_count FROM submissions "
					+ "WHERE learner_id='" + userId + "' AND course_run_id='" + courseId + "';"; 
		
		Statement stmt = edXConnection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		
		int quiz_attempts = 0;
		
		if (rs.next())
			quiz_attempts += rs.getInt("quiz_count");
				
		return quiz_attempts;
	}

	// Metric 5: ProportionTimeOnQuiz: the proportion of time spent on quiz pages from the total time spent on the platform
	// Unit: -
	// Calculation:
	// Tables used: "sessions" & "quiz_sessions". By which the "sessions" used fields are:
	//		"course_run_id" - to identify the course
	// 		"learner_id" - to identify sessions belonging to the current user
	// 		"duration" - to calculate the general time spent on platform
	//		*and the "quiz_sessions" table used fields are:
	//		"duration" - to calculate the time spent on quiz
	//		"course_run_id" - to identify the course
	// 		"learner_id" - to identify the current user
	
	private int getProportionTimeOnQuiz(Connection edXConnection, String courseId, String userId) throws SQLException {
		   String query_duration = "SELECT SUM(duration) AS total_time_duration FROM sessions WHERE course_run_id='" + courseId + "' AND learner_id='" + userId + "';";
		   String query_time_on_quiz = "SELECT SUM(duration) AS quiz_duration FROM quiz_sessions Where course_run_id='" + courseId + "' AND learner_id='" + userId + "';";
		   		   
		   Statement stmt = edXConnection.createStatement();
		   ResultSet rs;
		   
		   rs = stmt.executeQuery(query_duration);

		   int total_duration = 0;	//this fields is in seconds in the database
		  		   
		   if (rs.next())
			   total_duration = rs.getInt("total_time_duration");
		   		   
		   rs = stmt.executeQuery(query_time_on_quiz);
		   
		   int quiz_duration = 0;  //in seconds
		   
		   if (rs.next())
			   quiz_duration = rs.getInt("quiz_duration");
		   
		   return quiz_duration * 100 / total_duration;
	}

	// Metric 6: Timeliness: how early before the deadline learners submit their quiz question answers
	// Unit: hours
	// Calculation:
	// Tables used: "submissions" & ""quiz_questions". By which the "submission" used fields are:
	//		"course_run_id" - to identify the course
	// 		"learner_id" - to identify the current user
	//		"question_id" - to identify the current question
	//		"submission_timestamp" - to identify the user's quiz submission date 
	//		*and the "quiz_questions" table used fields are:
	//		"question_due" - to identify the quiz question due date
	//		"question_id" - to identify the current question
	
	private int getTimeliness(Connection edXConnection, String courseId, String userId) throws SQLException{
		String query = "SELECT COUNT(*) AS countLines"
				+ ", AVG(TIME_TO_SEC(TIMEDIFF(submissions.submission_timestamp, quiz_questions.question_due)) / 3600) AS timeliness_average"
				+ " FROM submissions"
				+ " INNER JOIN"
				+ " quiz_questions ON submissions.question_id = quiz_questions.question_id where"
				+ "submission.learner_id = '" + userId + "' AND submissions.course_run_id= '" + courseId + "';";
	
		Statement stmt = edXConnection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		
		double timeliness_average = 0;  //in hours
		 
		if (rs.next()) {
			timeliness_average += rs.getInt("timeliness_average");
			return timeliness_average;
		} while(rs.next());
		else
			return 0;		// in case rs == null
		}
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

	private String getUserId(Connection conn, String anonId) throws SQLException {		// todo: might not be needed, depending on the database structure and the values of the course_user_id fields
		//based on the anonymous user id available on the edX pages, connect to the short user id that is used in the trace logs
		String query = "SELECT * FROM learners WHERE anon_id='" + anonId + "';";
		String userId = "";

		Statement st = conn.createStatement();
		ResultSet res = st.executeQuery(query);

		if (res.next()) {
			userId = res.getString("user_id");
		}

		return userId;
	}

	//===============================
    //====== Script generation ======
	//===============================

   private String generateScript(String user_id, String values, String scaled_values, String last_week, String scaled_last_week, String this_week, String scaled_this_week) {

		String reply = "";

		if(Integer.parseInt(user_id) % 4 != 0)			// for the control group (userId modulo 4 == 0), do not show the widget
					reply = "$('#wrapper-widget').show();\n";

		reply += "\n" +
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
					"var values = " + values + ";\n" +
					"var lastWeek = " + last_week + ";\n" +
					"var thisWeek = " + this_week + ";\n" +
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
					"function timeStamp() {\n" +
					"  var now = new Date();\n" +
					"  var date = [ now.getFullYear(), now.getMonth() + 1, now.getDate() ];\n" +
					"  var time = [ now.getHours(), now.getMinutes(), now.getSeconds() ];\n" +
					"\n" +
					"  for ( var i = 1; i < 3; i++ ) {\n" +
					"    if ( time[i] < 10 ) {\n" +
					"      time[i] = \"0\" + time[i];\n" +
					"    }\n" +
					"  }\n" +
					"\n" +
					"  if( date[1] < 10 ) {\n" +
					"\tdate[1] = \"0\" + date[1];\n" +
					"  }\n" +
					"  \n" +
					"  return date.join(\"-\") + \"Z\" + time.join(\":\");\n" +
					"}\n" +
					"\n" +
					"function loadWidget() {\n" +
					"\t\n" +
					"\tvar user_id = '" + user_id + "';\n" +
					"\t\n" +
					"\t$('#container').highcharts({\n" +
					"\t\tchart: {\n" +
					"\t\t\tmarginTop: 120,\n" +
					"\t\t\tpolar: true,\n" +
					"\t\t\tstyle: {\n" +
					"\t\t\t\tfontFamily: 'Open Sans, sans-serif'\n" +
					"\t\t\t},\n" +
					"\t\t\ttype: 'area',\n" +
					"\t\t\tevents: {\n" +
					"\t\t\t\tload: function () {\n" +
					"\t\t\t\t\tvar category = user_id + '_week" + week + "';\n" +
					"\t\t\t\t\tgaPC('send', 'event', category, 'load_' + timeStamp());\n" +
					"\t\t\t\t}\n" +
					"\t\t\t}\n" +
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
					"\t\t{\n" +
					"\t\t\ttype: 'line',\n" +
					"\t\t\tname: 'Average graduate this week',\n" +
					"\t\t\tcolor: 'rgba(188, 64, 119, 0.5)',\n" +
					"\t\t\tdata: " + scaled_this_week + ",\n" +
					"\t\t\tvisible: false,\n" +
					"\t\t\tevents: {\n" +
					"\t\t\t\tshow: function () {\n" +
					"\t\t\t\t\tgaPC('send', 'event', user_id + '_week" + week + "', 'show-this-week_' + timeStamp());\n" +
					"\t\t\t\t},\n" +
					"\t\t\t\thide: function () {\n" +
					"\t\t\t\t\tgaPC('send', 'event', user_id + '_week" + week + "', 'hide-this-week_' + timeStamp());\n" +
					"\t\t\t\t}\n" +
					"\t\t\t}\n" +
					"\t\t},\n" +
					"\n" +
					"\t\t{\n" +
					"\t\t\tname: 'Average graduate last week',\n" +
					"\t\t\tcolor: 'rgba(255, 255, 102, 0.5)',\n" +
					"\t\t\tdata: " + scaled_last_week + ",\n" +
					"\n" +
					"\t\t\tevents: {\n" +
					"\t\t\t\tshow: function () {\n" +
					"\t\t\t\t\tgaPC('send', 'event', user_id + '_week" + week + "', 'show-last-week_' + timeStamp());\n" +
					"\t\t\t\t},\n" +
					"\t\t\t\thide: function () {\n" +
					"\t\t\t\t\tgaPC('send', 'event', user_id + '_week" + week + "', 'hide-last-week_' + timeStamp());\n" +
					"\t\t\t\t}\n" +
					"\t\t\t}\n" +
					"\n" +
					"\t\t},\n" +
					"\n" +
			"\t\t{\n" +
					"\t\t\tname: 'You',\n" +
					"\t\t\tcolor: 'rgba(144, 202, 249, 0.5)',\n" +
					"\t\t\tdata: " + scaled_values + ",\n" +
					"\t\t\tevents: {\n" +
					"\t\t\t\tshow: function () {\n" +
					"\t\t\t\t\tgaPC('send', 'event', user_id + '_week" + week + "', 'show-you_' + timeStamp());\n" +
					"\t\t\t\t},\n" +
					"\t\t\t\thide: function () {\n" +
					"\t\t\t\t\tgaPC('send', 'event', user_id + '_week" + week + "', 'hide-you_' + timeStamp());\n" +
					"\t\t\t\t}\n" +
					"\t\t\t}\n" +
					"\t\t}]\n" +
					"\t});\n" +
					"}\n" +
					"\t\t\n" +
					"loadWidget();";

			return reply;
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
