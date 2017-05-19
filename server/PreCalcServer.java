package precalc;
 
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.DriverManager;

public class PreCalcServlet extends HttpServlet {

    // database connection settings
    private static String dbURL = "jdbc:mysql://localhost:3306/learning_tracker";
    private static String dbDriver = "com.mysql.jdbc.Driver";
    private static String dbUser = "root";
    private static String dbPass = ***; //TODO: password needs to be updated

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response) 
		throws IOException, ServletException {
	PrintWriter writer = response.getWriter();
	String reply = "empty reply";
	String userId = "", 
		valuesString = "", 
		scaled_valuesString = "", 
		lastWeek = "", 
		scaled_lastWeek = "", 
		thisWeek = "", 
		scaled_thisWeek = "";
	double[] values = new double[6];
	double status = 0;

        try{
           String anonId = request.getParameter("anonId").toString();
	   int week = Integer.parseInt(request.getParameter("week"));
	
	   String query;
	   //get learner id from database
	   Connection conn = null; // connection to the database

	   // connects to the database
            Class.forName(dbDriver).newInstance();
            conn = DriverManager.getConnection(dbURL, dbUser, dbPass);


            //get user id
            query = "SELECT * FROM learners WHERE anon_id='" + anonId + "';";

	    Statement st = conn.createStatement();
            ResultSet res = st.executeQuery(query);
	  
            if (res.next()) {
                userId = res.getString("user_id");
            }
	   
	    //get values and scaled values
	    query = "SELECT * FROM precalc WHERE week='" + week + "' AND user_id='" + userId + "';"; 
	    
	    res = st.executeQuery(query);

	    if (res.next()) {
		status = res.getDouble(3);
		//learner's metric values
		for (int i = 0; i < values.length; i++)
		     values[i] = res.getDouble(i + 4);
		valuesString = composeString(values);

		//learner's scaled values
		for(int i = 0; i < values.length; i++)
		     values[i] = res.getDouble(i + 10);
		scaled_valuesString = composeString(values);
	    }

	    //get thresholds for last week
	    query = "SELECT * FROM precalc_thresholds WHERE week='" + week + "';";
	    res = st.executeQuery(query);

	    if(res.next()) {
		for(int i = 0; i < values.length; i++)
		    values[i] = res.getDouble(i+2);
		lastWeek = composeString(values);

		for(int i = 0; i < values.length; i++)
		    values[i] = res.getDouble(i+8);
		scaled_lastWeek = composeString(values);
	    }

	    //get thresholds for this week
	    query = "SELECT * FROM precalc_thresholds WHERE week='" + (week+1) + "';";
	    res = st.executeQuery(query);

	    if(res.next()) {
		for(int i = 0; i < values.length; i++)
		    values[i] = res.getDouble(i+2);
		thisWeek = composeString(values);

		for(int i = 0; i < values.length; i++)
		    values[i] = res.getDouble(i+8);
		scaled_thisWeek = composeString(values);
	    }

	    
	   reply = generateScript(week, userId, status, valuesString, scaled_valuesString, lastWeek, scaled_lastWeek, thisWeek, scaled_thisWeek);

 	//writer.write(anonId);
           writer.write(reply);
           
           }
       catch(Exception ex) {
              ex.getStackTrace();
       } finally {
         writer.close();  // Always close the output writer
       }
   }

    private String composeString(double[] values) {
        String result = "[";
        for(int i = 0; i < values.length - 1; i++)
            result += values[i] + ", ";
        result += values[values.length-1] + "]";

        return result;
    }

   private String generateScript(int week, String user_id, double status, String values, String scaled_values, String last_week, String scaled_last_week, String this_week, String scaled_this_week) {
        String text = getProgressMessage(user_id, status);
	String reply = "";

	if(Integer.parseInt(user_id) % 3 != 0)
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
                "\t\t\t\t\t\t\n" +
                "\t\tsubtitle: {\n" +
                "        \tuseHTML: true,\n" +
                "\t\t\tstyle: {\n" +
                "\t\t\t\talign: 'center'\n" +
                "\t\t\t},\n" +
                "            text: '" + text + "'\n" +
                "\t\t\t\n" +
                "        },\n" +
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

    public static String getProgressMessage(String user_id, double status) {
        if (Integer.parseInt(user_id) % 3 == 1)     //promotion
            if (status >= -1.0)
                return "<div style=\"border: 2px solid black; margin: 10px; padding: 5px; border-radius: 15px; text-align: center;\"> <b>Your Progress Summary:</b> <br> Looks like you\\\'re right <b>on track</b> to achieve your goal! Keep taking advantage of the exciting new topics each week. Always push yourself to be successful.</div>";
            else
                return "<div style=\"border: 2px solid black; margin: 10px; padding: 5px; border-radius: 15px; text-align: center;\"> <b>Your Progress Summary:</b> <br> Looks like you\\\'re a bit <b>behind</b> in achieving your goal! Work harder to take advantage of the exciting new topics each week. Always push yourself to be successful.</div>";
        else if (Integer.parseInt(user_id) % 3 == 2)    //prevention
            if (status >= -1.0)
                return "<div style=\"border: 2px solid black; margin: 10px; padding: 5px; border-radius: 15px; text-align: center;\"> <b>Your Progress Summary:</b> <br> Looks like you\\\'re <b>keeping up</b> with the course for now! We\\\'re doing our best to introduce you to exciting new topics each week. Please don\\\'t let us down now.</div>";
            else
                return "<div style=\"border: 2px solid black; margin: 10px; padding: 5px; border-radius: 15px; text-align: center;\"> <b>Your Progress Summary:</b> <br> Looks like you\\\'re a bit <b>behind</b> in the course right now! We\\\'re doing our best to introduce you to exciting new topics each week. Please don\\\'t let us down now.</div>";

        return "";
    }
}