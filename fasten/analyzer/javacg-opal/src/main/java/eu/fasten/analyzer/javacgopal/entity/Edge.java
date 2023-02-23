package eu.fasten.analyzer.javacgopal.entity;

import java.io.Serializable;
import java.util.Objects;

public class Edge implements Serializable {
    GraphNode from;
    GraphNode to;

    public Edge() {

    }

    public Edge(GraphNode from, GraphNode to) {
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

    public GraphNode getFrom() {
        return from;
    }

    public void setFrom(GraphNode from) {
        this.from = from;
    }

    public GraphNode getTo() {
        return to;
    }

    public void setTo(GraphNode to) {
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return from.equals(edge.from) && to.equals(edge.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
