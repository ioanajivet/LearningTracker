
class DatabaseData {
		protected String dbURL;
		protected String dbUser;
		protected String dbPass;
		protected String dbDriver;

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
