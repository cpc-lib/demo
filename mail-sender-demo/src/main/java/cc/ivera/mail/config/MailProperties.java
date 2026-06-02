package cc.ivera.mail.config;

/**
 * SMTP 连接配置。
 */
public class MailProperties {

    private String host;
    private int port = 587;
    private String fromAddress;
    private String fromName;
    private String username;
    private String password;
    private boolean auth = true;
    private boolean startTlsEnabled = true;
    private boolean sslEnabled = false;
    private boolean debug = false;
    private int connectionTimeoutMillis = 10_000;
    private int readTimeoutMillis = 30_000;
    private int writeTimeoutMillis = 30_000;

    public String getHost() {
        return host;
    }

    public MailProperties setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public MailProperties setPort(int port) {
        this.port = port;
        return this;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public MailProperties setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
        return this;
    }

    public String getFromName() {
        return fromName;
    }

    public MailProperties setFromName(String fromName) {
        this.fromName = fromName;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public MailProperties setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public MailProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public boolean isAuth() {
        return auth;
    }

    public MailProperties setAuth(boolean auth) {
        this.auth = auth;
        return this;
    }

    public boolean isStartTlsEnabled() {
        return startTlsEnabled;
    }

    public MailProperties setStartTlsEnabled(boolean startTlsEnabled) {
        this.startTlsEnabled = startTlsEnabled;
        return this;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public MailProperties setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public MailProperties setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public MailProperties setConnectionTimeoutMillis(int connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        return this;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public MailProperties setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
        return this;
    }

    public int getWriteTimeoutMillis() {
        return writeTimeoutMillis;
    }

    public MailProperties setWriteTimeoutMillis(int writeTimeoutMillis) {
        this.writeTimeoutMillis = writeTimeoutMillis;
        return this;
    }
}
