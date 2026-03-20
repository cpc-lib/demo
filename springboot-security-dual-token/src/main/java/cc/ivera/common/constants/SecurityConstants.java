package cc.ivera.common.constants;

public class SecurityConstants {
    /**
     * accessToken 的过期时间是1分钟 测试使用，一般设置30分钟
     */
    public static final long EXPIRATION_ASSESS_TOKEN = 1 * 60 * 60L;
    /**
     * 不记住密码refreshToken过期时间是1天
     */
    public static final long EXPIRATION_SHORT_REFRESH_TOKEN = 60 * 60 * 24L;
    /**
     * 记住密码的话refreshToken过期时间是30天
     */
    public static final long EXPIRATION_LONG_REFRESH_TOKEN = 60 * 60 * 24 * 30L;


    /**
     * JWT签名密钥硬编码到应用程序代码中，应该存放在环境变量或.properties文件中。
     */
    public static final String JWT_SECRET_KEY = "C*F-JaNdRgUkXn2r5u8x/A?D(G+KbPeShVmYq3s6v9y$B&E)H@McQfTjWnZr4u7w";

    // JWT token defaults
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "JWT";

    // Swagger WHITELIST
    public static final String[] SWAGGER_WHITELIST = {"/swagger-ui.html", "/swagger-ui/*", "/swagger-resources/**", "/v2/api-docs", "/v3/api-docs", "/webjars/**", "/doc.html", "/favicon.ico"};

    public static final String MY_API = "/api/**";

    // System WHITELIST
    public static final String[] SYSTEM_WHITELIST = {"/auth/login", "/users/sign-up"};

    private SecurityConstants() {
    }
}
