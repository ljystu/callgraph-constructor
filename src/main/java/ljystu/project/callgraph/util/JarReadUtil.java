package ljystu.project.callgraph.util;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarReadUtil {

    static void findTypeFiles(File dir, List<File> list, String type) {

        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    findTypeFiles(child, list, type);
                }
            }
        } else if (dir.getName().endsWith(type)) {
            list.add(dir);

        }
    }

    public static void extractClasses(String libraryPath, String tempDir) {
        try (ZipFile zipFile = new ZipFile(libraryPath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                File file = new File(tempDir, entry.getName());
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                try (InputStream input = zipFile.getInputStream(entry);
                     OutputStream output = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
