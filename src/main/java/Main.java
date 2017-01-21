import static spark.Spark.get;
import static spark.SparkBase.port;
import static spark.SparkBase.staticFileLocation;

import java.io.InputStream;
import java.net.URL;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

public class Main {

	public static void main(String[] args) {

		new Main().init();

	}

	public void init() {
		port(Integer.valueOf(System.getenv("PORT")));
		staticFileLocation("/public");

		get("/api/lineup/:id", (request, response) -> {

			String id = request.params(":id");
			Set<String> recivedLineUp = reciveLineUp(id);
			Set<JSONObject> recivedPossiblePlayers = recivePossiblePlayers("1");
			Map<String, Object> attributes = new HashMap<>();

			JSONArray data = new JSONArray();
			for (String playerName : recivedLineUp) {
				JSONArray matches = getMatches(recivedPossiblePlayers, playerName);
				JSONObject match = getMatched(matches, playerName);
				JSONObject temp = new JSONObject();
				try {
					temp.put("name", playerName);
					temp.put("matches", matches);
					temp.put("match", match);
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException("abbruch", e);
				}
				data.put(temp);
			}
			attributes.put("data", data.toString());

			return new ModelAndView(attributes, "json.ftl");
		} , new FreeMarkerEngine());
	}

	public JSONObject getMatched(JSONArray matches, String playerName) {
		if (matches.length() == 0) {
			return null;
		}
		if (matches.length() == 1) {
			try {
				return matches.getJSONObject(0);
			} catch (JSONException e) {
				e.printStackTrace();
				throw new RuntimeException("abbruch", e);
			}
		}
		String[] split = playerName.split(" ");
		String surname = split[0].replace(".", "");
		int length = surname.length();

		for (int i = 0; i < matches.length(); i++) {
			try {
				JSONObject player = matches.getJSONObject(i);
				String[] split2 = player.getString("name").split(" ");
				String surname2 = split2[0].replace(".", "");
				String shortsurname2 = surname2.substring(0, length);
				if(surname.equalsIgnoreCase(shortsurname2)){
					return player;
				}
			} catch (JSONException e) {
				e.printStackTrace();
				throw new RuntimeException("abbruch", e);
			}

		}
		return null;
	}

	public JSONArray getMatches(Set<JSONObject> recivedPossiblePlayers, String playerName) {
		JSONArray matched = new JSONArray();
		for (JSONObject player : recivedPossiblePlayers) {
			try {
				String[] split = playerName.split(" ");
				String lastname = split[split.length - 1];
				if (player.getString("lastname").equalsIgnoreCase(lastname)) {
					matched.put(player);
				}
			} catch (JSONException e) {
				e.printStackTrace();
				throw new RuntimeException("abbruch", e);
			}
		}
		return matched;
	}

	public Set<JSONObject> recivePossiblePlayers(String id) {
		if (id == null | id.trim().isEmpty()) {
			return new HashSet<>();
		}
		Set<JSONObject> playerList = new HashSet<JSONObject>();
		try {
			String json = null;
			String urlString = "http://football-api.florianmarsch.de/v1/api/league/" + id + "/players.json";
			InputStream is = (InputStream) new URL(urlString).getContent();
			json = IOUtils.toString(is, "ISO-8859-1");
			JSONArray players = new JSONArray(json);
			for (int i = 0; i < players.length(); i++) {
				JSONObject player = players.getJSONObject(i);
				playerList.add(player);
			}
			return playerList;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("abbruch", e);
		}
	}

	public Set<String> reciveLineUp(String id) {
		if (id == null | id.trim().isEmpty()) {
			return new HashSet<>();
		}
		try {
			String html = null;
			String urlString = "http://classic.comunio.de/playerInfo.phtml?pid=" + id;
			InputStream is = (InputStream) new URL(urlString).getContent();
			html = IOUtils.toString(is, "UTF-8");

			html = Normalizer.normalize(html, Normalizer.Form.NFD);
			html = html.replaceAll("[^\\p{ASCII}]", "");

			Document doc = Jsoup.parse(html);
			Elements lines = doc.select(".name_cont");

			Set<String> teamList = new HashSet<String>();
			for (int i = 0; i < lines.size(); i++) {
				Element line = lines.get(i);
				String tempName = line.html();
				tempName = StringEscapeUtils.unescapeHtml(tempName);
				String norm = Normalizer.normalize(tempName, Normalizer.Form.NFD);
				norm = norm.replaceAll("[^\\p{ASCII}]", "");
				String trim = norm.trim();

				teamList.add(trim);
			}
			System.out.println(teamList);
			return teamList;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("abbruch", e);
		}
	}

}
