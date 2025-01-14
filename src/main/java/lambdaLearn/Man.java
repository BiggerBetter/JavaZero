package lambdaLearn;

public class Man {
    private Godness godness;

    public Man(Godness godness) {
        this.godness = godness;
    }

    public Man() {
    }

    public Godness getGodness() {
        return godness;
    }

    public void setGodness(Godness godness) {
        this.godness = godness;
    }

    @Override
    public String toString() {
        return "Man{" +
                "godness=" + godness +
                '}';
    }
}
