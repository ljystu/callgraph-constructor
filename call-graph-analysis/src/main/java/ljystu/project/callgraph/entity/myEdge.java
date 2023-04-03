package ljystu.project.callgraph.entity;

import java.util.Objects;

public class myEdge {
    String id;
    Node startNode;
    Node endNode;
    String type;

    public myEdge() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        myEdge edge = (myEdge) o;
        return startNode.equals(edge.startNode) && endNode.equals(edge.endNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startNode, endNode);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Node getStartNode() {
        return startNode;
    }

    public void setStartNode(Node startNode) {
        this.startNode = startNode;
    }

    public Node getEndNode() {
        return endNode;
    }

    public void setEndNode(Node endNode) {
        this.endNode = endNode;
    }

    @Override
    public String toString() {
        return "myEdge{" +
                "id='" + id + '\'' +
                ", startNode=" + startNode +
                ", endNode=" + endNode +
                ", type='" + type + '\'' +
                '}';
    }
}
