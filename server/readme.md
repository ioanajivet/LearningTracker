# database changes
# Database Changes from edX to Vis_Server

Major changes:
- Removed course_run_id from functions since this course_run_id does not exist in the new database.
- Changed learner_id to course_learner_id 
- Changed submissions.submission_timestamp to submissions.sumission_timestamp
- Table change from course_run to courses
- Removed the course_run_id column from video_additional in the local database of idxmooc server

