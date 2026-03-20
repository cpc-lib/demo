package cc.ivera.service.impl;

import cc.ivera.properties.IPCntProperties;
import cc.ivera.service.IPCntService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class IPCntServiceImpl implements IPCntService {

    private static Map<String,Integer> ipCntMap = new HashMap<String,Integer>();

    @Autowired
    //request对象由使用starter的工程提供自动装配
    private HttpServletRequest httpServletRequest;

    @Override
    public void count() {
        String ip = httpServletRequest.getRemoteAddr();
        Integer count = ipCntMap.get(ip);
        if(count==null){
            ipCntMap.put(ip,1);
        }else{
            ipCntMap.put(ip,ipCntMap.get(ip)+1);
        }


    }

    @Autowired
    public IPCntProperties iPCntProperties;

    @Override
    //@Scheduled(cron="0/${tool.ip.cycle} * * * * ?") //读取yml配置，properties的属性存在无意义
    @Scheduled(cron="0/#{iPCntProperties.cycle} * * * * ?") //#{beanId.attr},需要在properties类上添加@Component注解，并使用@Import进行导入
    public void print() {
        if(iPCntProperties.getModel().equals(IPCntProperties.LogModel.DETAIL.getValue())){
            System.out.println(String.format("%15s%6s%15s","","IP访问监控",""));
            System.out.println("+---------IP ADR---------+---cnt---+");
            for (Map.Entry<String, Integer> entry : ipCntMap.entrySet()) {
                System.out.println(String.format("|%22s  |%6d   |",entry.getKey(),entry.getValue()));
            }
            System.out.println("+------------------------+---------+");
        }else if(iPCntProperties.getModel().equals(IPCntProperties.LogModel.SIMPLE.getValue())){
            System.out.println(String.format("%15s%6s%15s","","IP访问监控",""));
            System.out.println("+---------IP ADR---------+---------+");
            for (Map.Entry<String, Integer> entry : ipCntMap.entrySet()) {
                System.out.println(String.format("|%22s  |",entry.getKey()));
            }
            System.out.println("+------------------------+---------+");
        }
        if(iPCntProperties.getCycleReset()){
            ipCntMap.clear();
        }

    }
}
