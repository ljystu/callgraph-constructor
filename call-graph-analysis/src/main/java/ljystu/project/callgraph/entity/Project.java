package ljystu.project.callgraph.entity;

/**
 * @author ljystu
 */
public class Project {
    String name;
    String repoUrl;

    public Project() {
    }

    public Project(String name, String repoUrl) {
        this.name = name;
        this.repoUrl = repoUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }
}
