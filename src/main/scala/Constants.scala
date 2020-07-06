
object Constants {

  // Topic names used in this app
  val PAGE_VIEWS_TOPIC = "pageviews"
  val USERS_TOPIC = "users"
  val TOP_PAGES_TOPIC = "top_pages"

  // Column names used in this app
  val PGV_TIMESTAMP = "pageviews_timestamp"
  val USERS_TIMESTAMP = "users_timestamp"
  val PGV_USER_ID = "pageviews_userid"
  val USERS_USER_ID = "users_userid"
  val VIEW_TIME = "viewtime"
  val VIEW_TIME_SUM = "viewtime_sum"
  val TIMESTAMP = "timestamp"
  val USER_ID = "userid"
  val PAGE_ID = "pageid"
  val GENDER = "gender"
  val USERS_COUNT = "users_count"
  val VALUE = "value"
  val EVENT = "event"
  val GENDER_RANK = "g_rank"
  val WINDOW_RANK = "w_rank"
  val OUTPUT_COLUMNS = List("gender", "pageid", "viewtime_sum", "users_count")
}
