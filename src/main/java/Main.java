import static spark.Spark.get;
import static spark.Spark.put;
import static spark.SparkBase.port;
import static spark.SparkBase.staticFileLocation;

import java.net.URI;
import java.net.URISyntaxException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Main {

	public static void main(String[] args) {

		String REDIS_URL = System.getenv("REDIS_URL");
		if (REDIS_URL == null) {
			throw new RuntimeException("No REDIS_URL found");
		}

		JedisPool pool;
		try {
			System.out.println("Init REDIS :"+REDIS_URL);
			pool = getPool(new URI(REDIS_URL));
		} catch (URISyntaxException e) {
			throw new RuntimeException("Bad REDIS_URL", e);
		}

		String port = System.getenv("PORT");
		if (port == null || port.isEmpty()) {
			port = "80";
		}
		port(Integer.valueOf(port));
		staticFileLocation("/public");

		get("/api/:key", (request, response) -> {
			String key = request.params(":key");
			Jedis jedis = pool.getResource();
			String value = jedis.get(key);			
			return value;
		});
		
		put("/api/:key", (request, response) -> {
			String key = request.params(":key");
			String value = request.body();
			Jedis jedis = pool.getResource();
			String statusCode = jedis.set(key, value);
			return statusCode;
		});
	}

	public static JedisPool getPool(URI aURL) {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(10);
		poolConfig.setMaxIdle(5);
		poolConfig.setMinIdle(1);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		JedisPool pool = new JedisPool(poolConfig, aURL);
		return pool;
	}

}
