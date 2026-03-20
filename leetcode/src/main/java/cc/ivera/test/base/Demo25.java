package cc.ivera.test.base;

import cc.ivera.enums.CommentType;

public class Demo25 {
    public static void main(String[] args) {
        CommentType like = CommentType.LEMON;
        System.out.println(like.getType());
        like.print();
    }
}
