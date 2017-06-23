# Database Changes from edX to Vis_Server schema

### Major changes:
- Removed _course_run_id_ from functions since the _course_run_id_ does not exist in the new database.
- Changed _learner_id_ to _course_learner_id_ 
- Changed _submissions.submission_timestamp_ to _submissions.sumission_timestamp_
- Table change from _course_run_ to _courses_
- Removed the _course_run_id_ column from _video_additional_ table in the local database of idxmooc server

### Affected Function:
* getAverageTimePerWeek
* getLecturesRevisited
* getForumActivity
* getQuizAttempted
* getProportionTimeOnQuiz
* getTimeliness
* getCourseWeeks
* getCourseStartDate
* getCourseEndDate
* getTimeOnPlatform
* getForumContributions
* getQuizAttempted
* getTimeOnQuiz
* getTimeliness
* getWatchedVideosWithDuration
* getWatchedVideosCount
* getVideosLengths
