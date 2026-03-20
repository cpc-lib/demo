package cc.ivera.test.base;

public class Singleton {

    private volatile static Singleton instance; // 使用 volatile 关键字确保线程安全

    public static final Singleton SINGLETON = new Singleton();

    private Singleton() {
        // 私有构造函数，防止外部直接实例化
    }

    /**
     * 确保单例
     *
     * @return
     */
    public static Singleton getInstance() {
        // 第一重检查，非同步的
        if (instance == null) {
            // 第二重检查，同步的
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton(); // 在这里进行实例化操作
                }
            }
        }
        return instance;
    }

    /**
     * 无法确保是单例
     *
     * @return
     */
    public static Singleton getInstance2() {
        // 第一重检查，非同步的
        if (instance == null) {
            // 第二重检查，同步的
            synchronized (Singleton.class) {
                instance = new Singleton(); // 在这里进行实例化操作
            }
        }
        return instance;
    }
}