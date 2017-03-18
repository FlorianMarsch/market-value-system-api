import static spark.Spark.get;
import static spark.SparkBase.port;
import static spark.SparkBase.staticFileLocation;

import java.util.HashMap;
import java.util.Map;

import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

public class Main {

	public static void main(String[] args) {

		
		String port = System.getenv("PORT");
		if(port == null || port.isEmpty()){
			port ="80";
		}
		port(Integer.valueOf(port));
		staticFileLocation("/public");

		get("/api/test/:id", (request, response) -> {

			String id = request.params(":id");
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("data","Hello "+ id);
			return new ModelAndView(attributes, "json.ftl");
		} , new FreeMarkerEngine());
	}

}
