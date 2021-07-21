package Utils;

public class Apple {
    Integer ic;
    Integer id;
    String  Is;

    public Apple(Integer price, Integer number, String name) {
        this.price = price;
        this.number = number;
        this.name = name;
    }
    public Apple() {
        this.price = 1;
        this.number = 1;
        this.name = "Mac";
    }

    @Override
    public Apple clone() throws CloneNotSupportedException {
        return (Apple) super.clone();
    }

    public Integer getPrice() {
        return price;
    }
    public Integer getNumber() {
        return number;
    }
    public String getName() {
        return name;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
    public void setNumber(Integer number) {
        this.number = number;
    }
    public void setName(String name) {
        this.name = name;
    }
}
