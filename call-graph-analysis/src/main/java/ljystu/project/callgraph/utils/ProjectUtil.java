package ljystu.project.callgraph.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ljystu.project.callgraph.config.Constants;
import ljystu.project.callgraph.entity.Project;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The type Project downloader.
 */
@Slf4j
public class ProjectUtil {

    /**
     * Read projects list.
     *
     * @param filepath the filepath
     * @return the list
     */
// 读取文件并解析项目列表
    public static List<Project> readProjects(String filepath) {
        // 读取文件
        String json = readFile(filepath);
        // 解析项目列表
        ObjectMapper mapper = new ObjectMapper();
        List<Project> projects = new ArrayList<>();
        try {
            projects.addAll(mapper.readValue(json, new TypeReference<List<Project>>() {
            }));
        } catch (IOException e) {
            e.printStackTrace();
            log.error("projects not found");
        }

        return projects;
    }

    /**
     * Download and unzip string.
     *
     * @param project the project
     * @return the string
     */
// 下载项目并解压
    public static String downloadAndUnzip(Project project) {
        // 下载项目的zip文件
        URL url = null;
        String folderName = "";
        try {
            url = new URL(project.getRepoUrl() + Constants.PROJECT_LINK);

            File zipFile = new File(project.getName() + ".zip");
            Files.copy(url.openStream(), Paths.get(zipFile.getPath()));
            // 解压zip文件
            folderName = unzip(zipFile);
            // 删除zip文件
            zipFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return folderName;
    }

    public static String gitDownload(Project project) {

        String path = Constants.PROJECT_FOLDER + project.getName();
        try {
            String cloneCommand = "git clone " + project.getRepoUrl() + " " + project.getName();
            Process cloneProcess = Runtime.getRuntime().exec(cloneCommand, null, new File(Constants.PROJECT_FOLDER));
            cloneProcess.waitFor();
            System.out.println("Clone done");

            String cdCommand = "cd " + path;
            Process cdProcess = Runtime.getRuntime().exec(cdCommand);
            cdProcess.waitFor();


            String describeCommand = "git for-each-ref refs/tags --sort=-taggerdate --format '%(refname:short)' | head";
            Process describeProcess = Runtime.getRuntime().exec(describeCommand, null, new File(path));
            BufferedReader describeInput = new BufferedReader(new InputStreamReader(describeProcess.getInputStream()));
            String describeOutput = describeInput.readLine();


            describeProcess.waitFor();
            describeInput.close();
            if (describeOutput == null || describeOutput.startsWith("fatal")) {
                return path;
            }

            System.out.println("Latest tag: " + describeOutput);

            String switchCommand = "git checkout " + describeOutput.substring(1, describeOutput.length() - 1);
            Process switchProcess = Runtime.getRuntime().exec(switchCommand, null, new File(path));
            BufferedReader checkInput = new BufferedReader(new InputStreamReader(switchProcess.getInputStream()));
            String line;
            while ((line = checkInput.readLine()) != null) {
                System.out.println(line);
            }
            switchProcess.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return path;
    }

    // 读取文件内容
    private static String readFile(String filepath) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filepath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            log.error("Failed to read file:" + filepath);
            e.printStackTrace();
        }
        return sb.toString();
    }

    // 解压zip文件
    private static String unzip(File zipFile) throws IOException {
        String name = "";
        boolean flag = false;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(entry.getName());
                if (file.exists()) continue;
                if (!flag) {
                    name = entry.getName().substring(0, entry.getName().length() - 1);
                    flag = true;
                }
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    Files.copy(zis, Paths.get(file.getPath()));
                }
            }
        }
        return name;
    }

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

