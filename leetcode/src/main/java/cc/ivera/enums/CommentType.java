package cc.ivera.enums;

public enum CommentType {

    LIKE(1), COMMENT(2), LOVE(3), LEMON(4) {
        @Override
        public void print() {
            System.out.println("We save the world,today!");
        }
    };

    int type;

    CommentType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void print() {

    }
}
