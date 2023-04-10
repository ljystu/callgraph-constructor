package ljystu.project.callgraph.entity;

import java.util.Objects;

public class Edge {
    Node from;
    Node to;

    public Edge() {

    }

    public Edge(Node from, Node to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "from=" + from.toString() +
                ", to=" + to.toString() +
                '}';
    }

    public Node getFrom() {
        return from;
    }

    public void setFrom(Node from) {
        this.from = from;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Edge edge = (Edge) o;
        return from.equals(edge.from) && to.equals(edge.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    public Node getTo() {
        return to;
    }

    public void setTo(Node to) {
        this.to = to;
    }
}
