package Utils;

public class Apple {
    Integer ic;
    Integer id;
    String  Is;

    public Apple(Integer ic, Integer id, String is) {
        this.ic = ic;
        this.id = id;
        this.Is = is;
    }
    public Apple() {
        this.ic = 1;
        this.id = 1;
        this.Is = "Str";
    }

    public Integer getIc() {
        return ic;
    }

    public void setIc(Integer ic) {
        this.ic = ic;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIs() {
        return Is;
    }

    public void setIs(String Is) {
        this.Is = Is;
    }
}
