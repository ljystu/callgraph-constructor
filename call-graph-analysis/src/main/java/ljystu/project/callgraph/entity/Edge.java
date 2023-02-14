package ljystu.project.callgraph.entity;

public class Edge {
    Node from;
    Node to;
    public Edge(){

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

    public Node getTo() {
        return to;
    }

    public void setTo(Node to) {
        this.to = to;
    }
}
