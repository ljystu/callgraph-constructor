package eu.fasten.analyzer.javacgopal.entity;


import java.io.Serializable;
import java.util.Objects;

public class GraphNode implements Serializable {
    private String packageName;
    private String className;
    private String methodName;
    private String params;
    private String returnType;

    private String coordinate;

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return Objects.equals(packageName, graphNode.packageName) && Objects.equals(className, graphNode.className) && Objects.equals(coordinate, graphNode.coordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, className, coordinate);
    }

    public GraphNode() {

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

    public GraphNode(String packageName, String className, String methodName, String params, String returnType, String coordinate) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.params = params;
        this.returnType = returnType;
        this.coordinate = coordinate;
    }

    @Override
    public String toString() {
        return "GraphNode{" +
                "packageName='" + packageName + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", params='" + params + '\'' +
                ", returnType='" + returnType + '\'' +
                '}';
    }
}
