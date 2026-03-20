package cc.ivera.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * token对象
 * @author LiYunFei
 * @date 2023/6/20 22:00
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Token {
    /**
     * accessToken的存在，保证了登录态的正常验证，因其过期时间的短暂也保证了账号的安全性
     */
    private String accessToken;
    /**
     * refreshToken的存在，保证了用户无需在短时间内进行反复登陆操作来保证登录态的有效性，
     * 同时也保证了活跃用户的登录态可以一直存续而不需要进行重新登录，
     * 反复刷新也防止某些不怀好意的人获取refreshToken后对用户账号进行动手动脚的操作
     */
    private String refreshToken;
}
