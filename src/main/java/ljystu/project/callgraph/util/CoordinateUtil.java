package ljystu.project.callgraph.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CoordinateUtil {
    public static String getCoordinate(String className, String version) {
        //TODO version 需要从jar包名称中获取
        String requestUrl = "https://search.maven.org/solrsearch/select?";

        Map<String, Object> params = new HashMap<>();
        params.put("className", className);
//        params.put("version", version);

        String httpResult = httpRequest(requestUrl, params);
        JSONObject jsonObject = new JSONObject(httpResult);
        JSONObject response = jsonObject.getJSONObject("response");

        if ((int) response.get("numFound") == 0) return "not found";
        JSONArray jsonArray = (JSONArray) response.get("docs");
        JSONObject dependency = jsonArray.getJSONObject(0);
        String coordinate = (String) dependency.get("id");

        return coordinate;
    }

    private static String httpRequest(String requestUrl, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(requestUrl + urlencode(params));
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.connect();

            InputStream inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                sb.append(str);
            }
            bufferedReader.close();
            inputStreamReader.close();

            inputStream.close();

            httpURLConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String urlencode(Map<String, Object> map) {
        StringBuilder s = new StringBuilder();
        s.append("q=fc:").append(map.get("className"));
//                .append("&v:").append(map.get("version"));
        return s.toString();
    }
}
