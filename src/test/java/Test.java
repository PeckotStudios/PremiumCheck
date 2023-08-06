import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public class Test {

    public enum DataType {
        SQLITE,
        YAML,
        JSON
    }

    public static void main(String[] args) {
        System.out.println("Hello World!");
        System.out.println(DataType.valueOf("SQLITE"));
//        String json = "{\"a\":[{\"b\":{\"c\":\"value\"}}]}";
//        JSONObject obj = JSONObject.parseObject(json);
//        System.out.println(getValueByPath(obj, "a[0].b.c", String.class));
    }

    public static <T> T getValueByPath(JSONObject jsonObject, String path, Class<T> clazz) {
        if (jsonObject == null || path == null || path.isEmpty() || clazz == null) {
            return null;
        }
        String[] pathSegments = path.split("\\.");
        for (int i = 0; i < pathSegments.length; i++) {
            String segment = pathSegments[i];
            if (segment.isEmpty()) {
                return null; // Invalid segment
            }
            int indexStart = segment.indexOf("[");
            if (indexStart >= 0) {
                String key = segment.substring(0, indexStart);
                int indexEnd = segment.indexOf("]", indexStart);
                if (indexEnd < 0) {
                    return null; // Invalid segment
                }
                int index = Integer.parseInt(segment.substring(indexStart + 1, indexEnd));
                JSONArray jsonArray = jsonObject.getJSONArray(key);
                if (jsonArray == null || index < 0 || index >= jsonArray.size()) {
                    return null; // Invalid index or key not found
                }
                jsonObject = jsonArray.getJSONObject(index);
            } else {
                Object value = jsonObject.get(segment);
                if (i == pathSegments.length - 1 && value != null) {
                    return clazz.cast(value);
                }
                jsonObject = jsonObject.getJSONObject(segment);
                if (jsonObject == null) {
                    return null; // Key not found or not an object
                }
            }
        }
        return clazz.cast(jsonObject); // Use toString() to get the String value
    }



}
