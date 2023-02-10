package ljystu.project.callgraph.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaReadUtil {
    public static HashSet<String> getClasses(String dirPath) {
        HashSet<String> classes = new HashSet<>();

        File dir = new File(dirPath);

        if (!dir.isDirectory()) {
            System.out.println("The given path is not a directory.");
            return classes;
        }

        // 获取目录下的所有Java文件
        List<File> files = new ArrayList<>();
        findClassFiles(dir, files, ".java");

        // 正则表达式，用于匹配import语句
        Pattern importPattern = Pattern.compile("^import\\s(static\\s)?+(.+);$");

        for (File file : files) {
            Path path = file.toPath();
            String content = "";
            try {
                content = Files.readString(path);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            String[] lines = content.split("\n");
            for (String line : lines) {
                Matcher matcher = importPattern.matcher(line);
                if (matcher.matches()) {
                    classes.add(matcher.group(2));
                }
            }
        }
        return classes;
    }

    static void findClassFiles(File dir, List<File> list, String type) {

        if (dir.isDirectory()) {
            // 如果是目录，则遍历它的子目录
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    findClassFiles(child, list, type);
                }
            }
        } else if (dir.getName().endsWith(type)) {
            // 如果是java文件，则加入列表
            list.add(dir);

        }
    }
}
