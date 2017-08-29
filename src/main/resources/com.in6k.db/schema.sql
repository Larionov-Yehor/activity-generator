CREATE TABLE application_activities (
  time_start DateTime,
  time_end DateTime,
  time_zone String,
  offset UInt64,
  process_id UInt64,
  domain_id UInt64,
  user_id UInt64,
  project_id UInt64,
  is_offline UInt8,
  is_automatic UInt8,
  duration UInt64,
  screenshot_id String,
  window_title String,
  date Date MATERIALIZED toDate(time_start)
) ENGINE = MergeTree(date, (domain_id, user_id, project_id, is_offline), 8192);