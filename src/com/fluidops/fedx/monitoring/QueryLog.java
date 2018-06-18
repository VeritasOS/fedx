/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.monitoring;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.fluidops.fedx.structures.QueryInfo;

/**
 * Convenience class which writes the query backlog to a file, 
 * default: logs/queryLog.log
 * 
 * @author Andreas Schwarte
 *
 */
public class QueryLog
{
	public static Logger log = Logger.getLogger(QueryLog.class);
	
	private Logger queryLog;
	private File queryLogFile = new File("logs", "queryLog.log");

	public QueryLog() throws IOException {
		log.info("Initializing query log, output file: " + queryLogFile.getName());
		initQueryLog();
	}
	
	private void initQueryLog() throws IOException {
		queryLog = Logger.getLogger("QueryBackLog");
		queryLog.setAdditivity(false);
		queryLog.setLevel(Level.INFO);
		queryLog.removeAllAppenders();
		
		Layout layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss}: %m%n");
		
		RollingFileAppender appender = new RollingFileAppender(layout, queryLogFile.getAbsolutePath(), true);
		appender.setMaxFileSize("1024KB");
		appender.setMaxBackupIndex(5);
		queryLog.addAppender(appender);		
	}
	
	public void logQuery(QueryInfo query) {
		queryLog.info(query.getQuery().replace("\r\n", " ").replace("\n", " "));
		if (log.isTraceEnabled())
			log.trace("#Query: " + query.getQuery());
	}

}
