package ljystu.project.callgraph.util;

import java.io.File;

public class DeleteUtil {
    /**
     * delete project directory
     *
     * @param dirFile the dir file
     */
    public static void deleteFile(File dirFile) {

        if (!dirFile.exists()) {
            return;
        }

        if (dirFile.isFile()) {
            dirFile.delete();
            return;
        } else {

            for (File file : dirFile.listFiles()) {
                deleteFile(file);
            }
        }

        dirFile.delete();
    }
}
