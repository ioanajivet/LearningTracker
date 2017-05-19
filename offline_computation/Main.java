import java.io.IOException;
import java.text.ParseException;

/**
 * Created by Ioana on 6/8/2016.
 */
public class Main {

    public static void main(String[] args) throws ParseException, IOException {
        //MetricComputation.computeThreshold("precalc", 12, "data\\precalc\\", 5);
        //MetricComputation.computeThreshold("dw", 10, "data\\old_MOOC\\dw\\", 5);

        MetricComputation.computeMetrics("precalc", 1, "data\\precalc\\");
        //MetricComputation.computeMetrics("dw", 10, "data\\old_MOOC\\dw\\");

        //System.out.println(args[0]);
        //MetricComputation.computeMetrics(args[0], Integer.parseInt(args[1]), args[2]);

    }
}
