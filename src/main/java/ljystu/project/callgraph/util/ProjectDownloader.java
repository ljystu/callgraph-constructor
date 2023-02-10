package ljystu.project.callgraph.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Slf4j
public class ProjectDownloader {

    // 读取文件并解析项目列表
    public List<Project> readProjects(String filepath) {
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

    // 下载项目并解压
    public String downloadAndUnzip(Project project) {
        // 下载项目的zip文件
        URL url = null;
        String folderName = "";
        try {
            url = new URL(project.getRepoUrl() + "/archive/refs/heads/master.zip");

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

    // 读取文件内容
    private String readFile(String filepath) {
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
    private String unzip(File zipFile) throws IOException {
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

}

