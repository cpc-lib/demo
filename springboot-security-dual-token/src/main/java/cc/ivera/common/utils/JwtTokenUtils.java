package cc.ivera.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.ObjectUtils;
import cc.ivera.common.constants.SecurityConstants;
import cc.ivera.common.exception.CustomException;
import cc.ivera.entity.Token;

import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;
import java.util.Date;

/**
 * JWT工具类
 *
 * @author LiYunFei
 */
public class JwtTokenUtils {


    /**
     * 生成足够的安全随机密钥，以适合符合规范的签名
     */
    private static final byte[] API_KEY_SECRET_BYTES = DatatypeConverter.parseBase64Binary(SecurityConstants.JWT_SECRET_KEY);
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(API_KEY_SECRET_BYTES);


//    public static String createAccessToken(String username, String id) {
//        return createToken(username,id,SecurityConstants.EXPIRATION_ASSESS_TOKEN);
//    }

    /**
     * 一次性刷新两个token，返回token对象
     *
     * @author LiYunFei
     * @date 2023/6/20 22:15
     */
    public static Token createToken(String username, String id, boolean isRememberMe) {
        Token token = new Token(createToken(username, id, SecurityConstants.EXPIRATION_ASSESS_TOKEN), createToken(username, id, isRememberMe ? SecurityConstants.EXPIRATION_LONG_REFRESH_TOKEN : SecurityConstants.EXPIRATION_SHORT_REFRESH_TOKEN));
        return token;
    }

    public static Token refreshToken(String refreshToken) {
        Claims claims = getClaims(refreshToken);
        Object timeObj = claims.get("time");
        if (ObjectUtils.isEmpty(timeObj)) {
            throw new CustomException("认证异常");
        }
        Integer timeInt = (Integer) timeObj;
        long i = timeInt.intValue();
        Long time = new Long(i);
        return new Token(createToken(claims.getSubject(), claims.getId(), SecurityConstants.EXPIRATION_ASSESS_TOKEN), createToken(claims.getSubject(), claims.getId(), time));
    }

    private static String createToken(String username, String id, Long expiration) {
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + expiration * 1000);
        String tokenPrefix = Jwts.builder().setHeaderParam("type", SecurityConstants.TOKEN_TYPE).signWith(SECRET_KEY, SignatureAlgorithm.HS256).setId(id).setIssuer("arhi").setIssuedAt(createdDate).setSubject(username)
                //存本次长token的有效时间
                .claim("time", expiration).setExpiration(expirationDate).compact();
        return SecurityConstants.TOKEN_PREFIX + tokenPrefix; // 添加 token 前缀 "Bearer ";
    }

    public static String getId(String token) {
        Claims claims = getClaims(token);
        return claims.getId();
    }


    public static UsernamePasswordAuthenticationToken getAuthentication(String token) {
        Claims claims = getClaims(token);
        String userName = claims.getSubject();
        return new UsernamePasswordAuthenticationToken(userName, null, null);
    }


    private static Claims getClaims(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

}
