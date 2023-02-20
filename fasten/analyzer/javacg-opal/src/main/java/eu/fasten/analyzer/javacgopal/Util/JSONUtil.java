package eu.fasten.analyzer.javacgopal.Util;

import com.alibaba.fastjson.JSON;

public class JSONUtil {

    /*
     * 将 pojo 对象转为 json 字符串，并且驼峰命名修改为下划线命名
     */
    public static String buildData(Object bean) {
        try {
            return JSON.toJSONString(bean);
        } catch (Exception e) {
            return null;
        }
    }
}
