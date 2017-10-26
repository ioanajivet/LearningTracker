# Learning Tracker

## Functional architecture

The working architecture of the Learning Tracker has three components as shown in the figure below.

1. edX component - integrating the Learning Tracker on edX course pages (JavaScript).
2. Server backend - hosting a Tomcat servlet that generates the Learning Tracker script for each learner when requests are made from the edX course pages (Java8).
3. Local component - computing the information to be displayed on the widget based on the data extracted from the trace logs of learners (Java8).


![Technical architecture](images/LT_working_architecture.png)

### 1. edX component - widget script
In essence, the Learning Tracker is a JavaScript script embedded in the edX MOOC pages as part of the course material. The widget is plotted using [Highcharts](highcharts.com). The documentation for customizing the widget is [here](http://api.highcharts.com/highcharts).

An example of a widget script is included in the file [edx_integration](https://github.com/ioanajivet/LearningTracker/blob/master/edx_integration).

### 2. Server backend - data storing and script generation
The server backend serves two purposes:

1. storing online the learner data that is to be displayed on the widget
2. generating the widget script for a learner

**Storing learner data**
The data is stored in a mySQL database and updated weekly. 

**Generating widget scripts**
The server also accepts HTTPS requests from the edX pages that request the script of a specific learnerss. The request has two parameters: _learner's anonymous id_ and _the week_. The server backend is implemented as a Tomcat servlet that receives HTTPS requests and responds with the generated Learning Tracker script as a string. 

Any changes in the widget interface design should be made in the script generation Tomcat servlet.

## 3. Local component - metric calculation

The offline component is used for calculating the metrics to be displayed on the widget based on data extracted from edX trace logs:

1. calculating the _average graduate_ profile
2. calculating the profile for each learner

The code for the local, offline component is included in the folder metric_calculation. he code for generating the metric values and the script are customizable for every run of the experiment. The current implementation calculates 13 metrics as presented below. The metrics to be displayed on the Learning Tracker are selected in the `initialize` method of the `MetricComputation` class.  

### Metrics available
The metrics are calculated considering data generated from the first day of the course.

1. Number of sessions per week
2. Average length of a session (in minutes)
3. Average time between sessions (in hours)
4. Number of forum session
5. Number of quiz questions attempted
6. Timeliness of quiz answers submission (in hours)
7. Number of sessions logged
8. Number of videos accessed
9. Time spent on the platform (in seconds)
10. Average time spent on the platform per week (in seconds)
11. Proportion of time spent on assignments (graded quiz questions)
12. Number of video lectures re-visited
13. Number of forum contributions

### Threshold calculation

Thresholds are calculated using the method `computeThreshold` in the class `MetricComputation`. The results will be placed in the folder `thresholds` and used in the subsequent steps.

