package cc.ivera.service;

public final class MessageConsumeClaim {

    public enum Status {
        CLAIMED,
        CONSUMED,
        BUSY
    }

    private final Status status;

    private final String leaseToken;

    private MessageConsumeClaim(Status status, String leaseToken) {
        this.status = status;
        this.leaseToken = leaseToken;
    }

    public static MessageConsumeClaim claimed(String leaseToken) {
        return new MessageConsumeClaim(Status.CLAIMED, leaseToken);
    }

    public static MessageConsumeClaim consumed() {
        return new MessageConsumeClaim(Status.CONSUMED, null);
    }

    public static MessageConsumeClaim busy() {
        return new MessageConsumeClaim(Status.BUSY, null);
    }

    public Status getStatus() {
        return status;
    }

    public String getLeaseToken() {
        return leaseToken;
    }
}
