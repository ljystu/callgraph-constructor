package ljystu.project.callgraph.entity;


import java.util.Objects;

/**
 * @author ljystu
 */
public class Node {
    private String packageName;
    private String className;
    private String methodName;
    private String params;
    private String returnType;


    String origin;
    String coordinate;
    String accessModifier;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node) o;
        return Objects.equals(packageName, node.packageName) && Objects.equals(className, node.className)
                && Objects.equals(methodName, node.methodName) && Objects.equals(params, node.params) &&
                Objects.equals(returnType, node.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, className, methodName, params, returnType);
    }

    @Override
    public String toString() {
        return "Node{" +
                "packageName='" + packageName + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", params='" + params + '\'' +
                ", returnType='" + returnType + '\'' +
                ", coordinate='" + coordinate + '\'' +
                '}';
    }

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Node(String packageName, String className, String methodName, String params, String returnType) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.params = params;
        this.returnType = returnType;
    }

    public Node(String packageName, String className, String methodName, String params, String returnType, String origin) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.params = params;
        this.returnType = returnType;
        this.origin = origin;
    }

    public String getAccessModifier() {
        return accessModifier;
    }

    public void setAccessModifier(String accessModifier) {
        this.accessModifier = accessModifier;
    }

    public Node() {

    }
}
