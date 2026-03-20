package cc.ivera.test.base;

import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import cc.ivera.util.PinYin4JUtil;

import java.util.Arrays;

/**
 * pinyin工具的使用
 */
public class Demo8 {
    public static void main(String[] args) throws BadHanyuPinyinOutputFormatCombination {
        String[] strs = PinYin4JUtil.stringToPinyin("南京潘西");
        System.out.println(Arrays.asList(strs));
        String pinyin = PinYin4JUtil.hanziToPinyin("南京潘西");
        System.out.println(pinyin);
        String[] firstChar = PinYin4JUtil.getHeadByString("南京潘西", true, null);
        System.out.println(Arrays.asList(firstChar));
        String headCharStr = PinYin4JUtil.getHeadCharStr(firstChar);
        System.out.println(headCharStr);
    }
}
