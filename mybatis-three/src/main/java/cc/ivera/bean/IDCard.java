package cc.ivera.bean;

public class IDCard {
    /**
     * 主键id
     */
    private String id;
    /**
     *  身份证号
     */
    private String number;

    /**
     * 所属人的对象
     */
    private Person p;

    public IDCard() {
    }

    public IDCard(String id, String number, Person p) {
        this.id = id;
        this.number = number;
        this.p = p;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Person getPerson() {
        return p;
    }

    public void setPerson(Person p) {
        this.p= p;
    }

    @Override
    public String toString() {
        return "Card{" +
                "id=" + id +
                ", number='" + number + '\'' +
                ", person=" + p +
                '}';
    }
}
