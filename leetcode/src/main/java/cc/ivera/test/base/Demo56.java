package cc.ivera.test.base;

public class Demo56 {
    public static void main(String[] args) {
        Character character = Character.valueOf('a');
        String s = Character.toString(97);
        if (s.equals(character.toString())) {
            System.out.println(true);
        }
    }
}
