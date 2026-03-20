package cc.ivera.DMA9_外观模式;

import cc.ivera.DMA9_外观模式.DM9.FacadeAB;
import cc.ivera.DMA9_外观模式.DM9.ModuleA.FacadeA;
import cc.ivera.DMA9_外观模式.DM9.ModuleA.SubSystemA;
import cc.ivera.DMA9_外观模式.DM9.ModuleA.SubSystemB;
import cc.ivera.DMA9_外观模式.DM9.ModuleB.FacadeB;
import cc.ivera.DMA9_外观模式.DM9.ModuleB.SubSystemC;
import cc.ivera.DMA9_外观模式.DM9.ModuleB.SubSystemD;
import org.junit.jupiter.api.Test;

/**
 * <p>外观模式测试</p>
 *
 * @Author Appleyk
 * @Blob https://blog.csdn.net/appleyk
 * @Date Created on 上午 8:30 2018-11-12
 * @Version V.1.0.1
 */
public class FacadeTest {


    /**
     * 通过以上的对比，想必大家已经知道了什么时候该用外观模式，什么时候直接调用子模块
     * 另外插一句：装饰模式、代理模式以及刚刚才学到的外观模式，其模板就是参照的对象适配器来着的
     * 因此，你会有种似曾相识的感觉
     * 设计模式不是一天两天就能悟的透的，它是一个长期而漫长的过程
     * 一旦你领悟到其中的门道，开发项目+写代码将会是一门艺术
     */
    @Test
    public void test1() {
        /**
         * 不使用外观模式，直接使用智能人工系统的模块A和模块B的功能
         * 缺点：用户必须清楚模块中的各个子系统的工作流程，否则会导致系统的不正常工作
         */
        useModuleA();
        useModuleB();
    }


    @Test
    public void test2(){
        /**
         * 使用外观模式A和外观模式B
         * 缺点：依然不够简洁
         */
        useModuleAByFacadeA();
        useModuleAByFacadeB();
    }


    @Test
    public void test3(){
        /**
         * 使用外观模式AB
         * 优点：用户使用系统，相当的便捷，没有多余的废话
         */
        useSystemByFacadeAB();
    }


    /**
     * 不使用外观模式，直接使用模块A的功能
     */
    private static void useModuleA(){
        partition("直接使用模块A中的功能");
        SubSystemA subSystemA = new SubSystemA();
        SubSystemB subSystemB = new SubSystemB();
        subSystemA.initSystem();
        subSystemB.loadDatas();
    }

    /**
     * 不使用外观模式，直接使用模块B的功能
     */
    private static void useModuleB(){
        partition("直接使用模块B中的功能");
        SubSystemC subSystemC = new SubSystemC();
        SubSystemD subSystemD = new SubSystemD();
        subSystemC.sayHello();
        subSystemD.working();
    }

    /**
     * 使用外观模式A，对模块A中的功能进行"屏蔽"
     */
    private static void useModuleAByFacadeA(){
        partition("使用外观模式A");
        FacadeA facadeA = new FacadeA();
        facadeA.initialize();
    }

    /**
     * 使用外观模式B，对模块B中的功能进行"屏蔽"
     */
    private static void useModuleAByFacadeB(){
        partition("使用外观模式A");
        FacadeB facadeB = new FacadeB();
        facadeB.work();
    }

    /**
     * 使用外观模式AB，对外观模式A和B再进行一次"屏蔽"
     */
    private static void useSystemByFacadeAB(){
        partition("使用外观模式AB");
        FacadeAB facadeAB = new FacadeAB();
        facadeAB.startSystem();
    }


    private static  void partition(String note){
        System.out.println("============== 分割线【"+note+"】 ==============");
    }
}
