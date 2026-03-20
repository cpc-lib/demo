package cc.ivera.util;

import java.util.ArrayList;
import java.util.List;

public class SqlUtils {
    /**
     * 平均拆分list方法.
     * @param source
     * @param n
     * @param <T>
     * @return
     */
    public static <T> List<List<T>> averageAssign(List<T> source, int n){
        List<List<T>> result=new ArrayList<List<T>>();
        //remainder余数
        //source = 100 n=3
        int remainder=source.size()%n;
        //平均每个子list的长度
        //33
        int number=source.size()/n;
        int offset=0;
        //0 0*33+0=0 (0+1)*33+0+1=34 remainder=0 offset=1
        //1 1*33+1=34 (1+1)*33+1=67 remainder=0 offset=1
        //2 2*33+1=67 3*33+1=100 remainder=0 offset=1
        for(int i=0;i<n;i++){
            List<T> value=null;
            if(remainder>0){
                value=source.subList(i*number+offset, (i+1)*number+offset+1);
                remainder--;
                offset++;
            }else{
                value=source.subList(i*number+offset, (i+1)*number+offset);
            }
            result.add(value);
        }
        return result;
    }
}
