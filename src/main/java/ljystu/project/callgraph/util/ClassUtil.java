package ljystu.project.callgraph.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClassUtil {

    // 存储所有class文件的列表
    private List<String> classFiles = new ArrayList<>();

    // 构造函数，传入要遍历的目录
    public ClassUtil(File dir) {
        findClassFiles(dir);
    }

    // 递归遍历目录，找到所有class文件
    private void findClassFiles(File dir) {
        if (dir.isDirectory()) {
            // 如果是目录，则遍历它的子目录
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    findClassFiles(child);
                }
            }
        } else if (dir.getName().endsWith(".class")) {
            // 如果是class文件，则加入列表
            String s = dir.toString().replace("/", ".");
            if(s.contains("$")){
                classFiles.add(s.substring(s.lastIndexOf("target.class") + 15, s.lastIndexOf("$")));
            }
            else {
                classFiles.add(s.substring(s.lastIndexOf("target.class") + 15, s.lastIndexOf(".")));
            }

        }
    }

    // 获取所有class文件的列表
    public List<String> getClassFiles() {
        return classFiles;
    }

}

// 使用示例

