package model;

public class DSAParameters {
    private String p;
    private String q;
    private String g;
    private String x;
    private String y;
    private String createdAt;

    public DSAParameters() {
    }

    public DSAParameters(String p, String q, String g, String x, String y, String createdAt) {
        this.p = p;
        this.q = q;
        this.g = g;
        this.x = x;
        this.y = y;
        this.createdAt = createdAt;
    }

    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
