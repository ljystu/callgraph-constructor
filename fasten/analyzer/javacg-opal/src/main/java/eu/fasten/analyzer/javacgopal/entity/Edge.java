package eu.fasten.analyzer.javacgopal.entity;

import java.io.Serializable;

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
}
