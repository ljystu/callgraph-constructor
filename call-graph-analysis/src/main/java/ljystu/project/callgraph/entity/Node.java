package ljystu.project.callgraph.entity;


public class Node {
    private String packageName;
    private String className;
    private String methodName;
    private String params;
    private String returnType;

    String coordinate;

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

    public Node(String packageName, String className, String methodName, String params, String returnType) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.params = params;
        this.returnType = returnType;
    }

    public Node(){

    }
}
