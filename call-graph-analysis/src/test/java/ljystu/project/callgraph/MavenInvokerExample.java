package ljystu.project.callgraph;

import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class MavenInvokerExample {
    public static void main(String[] args) throws IOException, MavenInvocationException {
        File pom = new File("pom.xml");

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pom);
        request.setGoals(Collections.singletonList("dependency:list"));

        File outputFile = new File("dependency-list.txt");
//        request.setOutputHandler(new RedirectingOutputHandler(outputFile));

        Invoker invoker = new DefaultInvoker();
        invoker.execute(request);

        ArrayList<String> dependencyList = new ArrayList<>();
        Scanner scanner = new Scanner(outputFile);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            dependencyList.add(line);
        }
        scanner.close();

        System.out.println("Dependency List: " + dependencyList);
    }
}
