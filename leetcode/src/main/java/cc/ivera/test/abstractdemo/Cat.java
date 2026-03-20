package cc.ivera.test.abstractdemo;

public  abstract class Cat {
    public Cat(){
        System.out.println("Hello world!");
    }

    public static void sayHello(){
        System.out.println("Hello!");
    }

    public abstract void test();
}
