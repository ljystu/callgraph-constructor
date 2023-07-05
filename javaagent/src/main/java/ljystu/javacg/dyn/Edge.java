package ljystu.javacg.dyn;

import java.io.Serializable;
import java.util.Objects;

public class Edge implements Serializable {
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
        return "{" +
                "from:" + from.toString() +
                ", to:" + to.toString() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(from, edge.from) && Objects.equals(to, edge.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    public Node getFrom() {
        return from;
    }

    public void setFrom(Node from) {
        this.from = from;
    }

    public Node getTo() {
        return to;
    }

    public void setTo(Node to) {
        this.to = to;
    }
}
