package edu.nrao.companalyst;

import java.time.Duration;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class CompAnalystCache {
	
	private JedisPool jedisPool;
	private JedisPoolConfig config;
	
	private String host;
	private int port;
	private int timeout;

	public CompAnalystCache(String host, int port, int timeout) {
		super();
		this.host = host;
		this.port = port;
		this.timeout = timeout;
		this.config = buildPoolConfig();
		this.jedisPool = new JedisPool(this.config, host, port, timeout);
	}

	private JedisPoolConfig buildPoolConfig() {
	    final JedisPoolConfig poolConfig = new JedisPoolConfig();
	    poolConfig.setMaxTotal(128);
	    poolConfig.setMaxIdle(128);
	    poolConfig.setMinIdle(16);
	    poolConfig.setTestOnBorrow(true);
	    poolConfig.setTestOnReturn(true);
	    poolConfig.setTestWhileIdle(true);
	    poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
	    poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
	    poolConfig.setNumTestsPerEvictionRun(3);
	    poolConfig.setBlockWhenExhausted(true);
	    return poolConfig;
	}

	public String getJson(String key) {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			String json = jedis.get(key);
			return json;
		} finally {
			if (jedis != null) jedis.close();
		}
	}
	
	private void saveJson(String key, String json) {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			jedis.set(key, json);
		} finally {
			jedis.close();
		}
	}
	public String getCompanyJob(String jobCode) {
		String key = "companyjob:" + jobCode;
		String json = getJson(key);
		return json;
	}

	public void saveCompanyJob(String jobCode, String json) {
		String key = "companyjob:" + jobCode;
		saveJson(key, json);
	}
	
	public String getCompanyJobList() {
		String key = "companyjoblist";
		String json = getJson(key);
		return json;
	}

	public void saveCompanyJobList(String json) {
		String key = "companyjoblist";
		saveJson(key, json);
	}
	
	public String getEmployeeList() {
		String key = "employeelist";
		String json = getJson(key);
		return json;
	}

	public void saveEmployeeList(String json) {
		String key = "employeelist";
		saveJson(key, json);
	}
	

	public String getCompensationJobList() {
		String key = "compensationjoblist";
		String json = getJson(key);
		return json;
	}

	public void saveCompensationJobList(String json) {
		String key = "compensationjoblist";
		saveJson(key, json);
	}
	
}
