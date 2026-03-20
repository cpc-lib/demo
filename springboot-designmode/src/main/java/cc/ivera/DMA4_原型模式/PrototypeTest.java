package cc.ivera.DMA4_原型模式;

import cc.ivera.DMA4_原型模式.DM4.HomeWork;
import cc.ivera.DMA4_原型模式.DM4.PupilStudent;
import cc.ivera.DMA4_原型模式.DM4.SexEnum;
import cc.ivera.DMA4_原型模式.DM4.WorkTypeEnum;
import cc.ivera.utils.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * <p>原型模式测试 == 两种方式，你选哪一种？</p>
 *
 * @Author Appleyk
 * @Blob https://blog.csdn.net/appleyk
 * @Date Created on 下午 12:58 2018-11-8
 * @Version V.1.0.1
 */
public class PrototypeTest {

    @Test
    public void test1() throws CloneNotSupportedException {

        // 原型 == 我们创建一个已经由小学生【刘晓然】完成的作业对象
        HomeWork homeWork = new HomeWork();
        // 设置作业信息
        homeWork.setType(WorkTypeEnum.WULI);
        homeWork.setPages(12);
        homeWork.setFinishTime(new Date());
        // 设置小学生信息 == 刘晓然
        PupilStudent pupilStudent = new PupilStudent();
        pupilStudent.setsNo(1001L);
        pupilStudent.setName("刘晓然");
        pupilStudent.setAge(10);
        pupilStudent.setSex(SexEnum.FEMALE);
        pupilStudent.setsClass(4);
        homeWork.setPupilStudent(pupilStudent);

        // 1、原型模式第一种 == 作业对象浅拷贝测试(地址引用相同,完成人信息一致)
        HomeWork ykHomeWork = shallowCopy(homeWork);
        System.out.println("刘晓然的作业：\n" + homeWork);
        System.out.println("我的作业：\n" + ykHomeWork);
    }

    @Test
    public void test2() throws CloneNotSupportedException {
        // 原型 == 我们创建一个已经由小学生【刘晓然】完成的作业对象
        HomeWork homeWork = new HomeWork();
        // 设置作业信息
        homeWork.setType(WorkTypeEnum.WULI);
        homeWork.setPages(12);
        homeWork.setFinishTime(new Date());
        // 设置小学生信息 == 刘晓然
        PupilStudent pupilStudent = new PupilStudent();
        pupilStudent.setsNo(1001L);
        pupilStudent.setName("刘晓然");
        pupilStudent.setAge(10);
        pupilStudent.setSex(SexEnum.FEMALE);
        pupilStudent.setsClass(4);
        homeWork.setPupilStudent(pupilStudent);

        // 2、原型模式第二种 == 作业对象深拷贝测试
        HomeWork zhangHomeWork = deepCopy(homeWork);
        System.out.println("Appleyk的作业：\n" + homeWork);
        System.out.println("张聪明的作业：\n" + zhangHomeWork);
    }

    /**
     * 对象浅拷贝
     *
     * @param homeWork
     * @return
     * @throws CloneNotSupportedException
     */
    public static HomeWork shallowCopy(HomeWork homeWork) throws CloneNotSupportedException {

        /**
         *  独白：
         *  （1）复制一份【刘晓然】的作业
         *  （2）将复制过来的作业改成我自己的，记住，信息全改，不然被老师发现了，我还在"三好学生"堆里面怎么混
         *  （3）我以为自己耍了个小聪明，擅自改作业，却不知"尴尬"却正在发生....
         */
        HomeWork myHomeWork = homeWork.clone();

        // 开始改造  == 首先改完成时间
        myHomeWork.setFinishTime(DateUtils.addDays(1));
        // 然后改作业的完成者，就是我 == 【Appleyk】
        PupilStudent mySelf = myHomeWork.getPupilStudent();
        // 学号肯定不能一样吧，不然这还不被发现作业是抄的吗
        mySelf.setsNo(1002L);
        // 我去，还要改名字，这事我差点忘了
        mySelf.setName("Appleyk");
        // 性别，对，还有性别，这个不能粗心大意，忘改了
        mySelf.setSex(SexEnum.MALE);
        // OK，一切就绪，改的那叫一个相当顺利啊，哈哈哈哈！ == 满心欢喜交作业咯
        return myHomeWork;
    }

    /**
     * 对象深度拷贝
     *
     * @param homeWork
     * @return
     * @throws CloneNotSupportedException
     */
    public static HomeWork deepCopy(HomeWork homeWork) throws CloneNotSupportedException {

        /**
         *  独白：
         *  （1）复制一份【Appleyk】的作业
         *  （2）因为是深度拷贝，抄的比较认真，比较深，雪下的那么认真.....
         *  （3）因此，我完全不必担心，老师会发现端倪，哈哈哈哈哈哈.....
         */
        HomeWork myHomeWork = homeWork.deepClone();
        // 开始改造  == 首先改完成时间，完成时间+1天
        myHomeWork.setFinishTime(DateUtils.addDays(1));
        PupilStudent mySelf = myHomeWork.getPupilStudent();
        mySelf.setsNo(1003L);
        mySelf.setName("张聪明");
        mySelf.setSex(SexEnum.MALE);
        return myHomeWork;
    }
}
