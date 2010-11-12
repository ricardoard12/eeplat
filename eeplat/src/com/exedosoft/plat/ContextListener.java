package com.exedosoft.plat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.exedosoft.plat.bo.DODataSource;
import com.exedosoft.plat.util.DOGlobals;

public class ContextListener implements ServletContextListener {

	private static Log log = LogFactory.getLog(ContextListener.class);

	/**
	 * 关闭数据库连接池
	 */
	public void contextDestroyed(ServletContextEvent arg0) {

		log.info("contextDestroyed!!!!!");

		DODataSource defaultDs = DODataSource.parseGlobals();

		String sql = "select dds.* from DO_DataSource dds,DO_Application da where dds.applicationUID = da.objuid and da.name = ?";

		try {
			List list = DAOUtil.INSTANCE().select(DODataSource.class, sql,
					DOGlobals.getValue("application"));
			list.add(defaultDs);

			for (Iterator it = list.iterator(); it.hasNext();) {

				DODataSource dss = (DODataSource) it.next();

				// /以DODataSource 为key
				if (DODataSource.pools != null && dss != null) {
					BasicDataSource bds = (BasicDataSource) DODataSource.pools
							.get(dss);
					log.info("关闭数据库连接池::" + dss.getDriverUrl());
					if (bds != null) {
						bds.close();
						log.info("数据库连接池正常关闭！");
					}
				}

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 开启数据库连接池
	 */
	public void contextInitialized(ServletContextEvent arg0) {
		// TODO Auto-generated method stub

		System.setErr(new LoggerPrintStream("ExceptionOutPrint", "warn"));
		System.setOut(new LoggerPrintStream("SystemOutPrint", "info"));

		if ("serial".equals(DOGlobals.getValue("useSerial"))) {
			CacheFactory.getCacheData().fromSerialObject();

			// //应该可以从里从两个文件中加载

		}

		DODataSource defaultDs = DODataSource.parseGlobals();
		poolASource(defaultDs);

		// String sql =
		// "select dds.* from DO_DataSource dds,DO_Application da where dds.applicationUID = da.objuid and da.name = ?";
		try {

			System.out.println("Application's Name:: "
					+ DOGlobals.getValue("application"));

			List<DODataSource> list = DODataSource.getDataSourcesNeedInit();
			// DAOUtil.select(DODataSource.class, sql, DOGlobals
			// .getValue("application"));
			for (Iterator<DODataSource> it = list.iterator(); it.hasNext();) {

				DODataSource dss = (DODataSource) it.next();
				log.info("初始化数据库连接池::" + dss.getDriverUrl());

				if (dss.getDriverUrl().equals(defaultDs.getDriverUrl())) {
					log.info("...和初始化连接池相同，使用初始化连接池::" + dss.getDriverUrl());
					DODataSource.pools.put(dss, DODataSource.pools
							.get(defaultDs));
					continue;
				}

				poolASource(dss);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("数据库连接池启动失败::" + e.getMessage());
		}
	}

	private BasicDataSource poolASource(DODataSource dss) {
		// ////////////////////////////////////////////////////////////////////////
		// 根据配置文件
		if (dss.getOtherparas() != null && dss.getOtherparas().endsWith(".xml")) {
			dss = DODataSource.parseConfigHelper(dss.getOtherparas(), dss
					.getObjUid());
		}
		// /////////////////////////////////////////////////////

		BasicDataSource bds = new BasicDataSource();

		bds.setDriverClassName(dss.getDriverClass());
		bds.setUrl(dss.getDriverUrl());
		bds.setUsername(dss.getUserName());
		bds.setPassword(dss.getPassword());
		// 最小空闲连接
		// bds.setMinIdle(5);
		// 最大空闲连接
		bds.setMaxIdle(5);
		// 超时回收时间(以毫秒为单位)
		// //等待30秒
		bds.setMaxWait(30000);
		// /////////初始化连接池
		bds.setInitialSize(10);

		bds.setRemoveAbandoned(true);
		bds.setRemoveAbandonedTimeout(60);

		// //////////最大连接数
		if (dss.getPoolsize() != null) {
			bds.setMaxActive(dss.getPoolsize().intValue());
		} else {
			bds.setMaxActive(100);
		}
		DODataSource.pools.put(dss, bds);
		return bds;
	}

	public static void main(String[] args) {

		Logger logger = Logger.getLogger("SystemOutPrint");
		logger.info("test");

		System.out.println(new java.util.Date(1238647760783L).toLocaleString());
	}

}

class LoggerPrintStream extends PrintStream {

	Logger logger;
	String level = "info";

	public LoggerPrintStream(String logName, String level) {
		super(new ByteArrayOutputStream(0));
		logger = Logger.getLogger(logName);
		this.level = level;
		if (logger == null)
			throw new RuntimeException("Can't logger:" + logName);
	}

	public void println(String s) {
		if ("info".equals(level)) {
			logger.info(s);
		} else {
			logger.warn(s);
		}
	}
	// /其它它代码略.......
}
