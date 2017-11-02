package mattoncino.pollo;

/**
 * Constant values used by application's comunication protocol
 * between users/devices and/or for interprocess comunication.
 */
public final class Consts {
    public static final String REQUEST = "request";
    public static final String VOTE = "vote";
    public static final String ACCEPT = "accept";
    public static final String REJECT = "reject";
    public static final String RECEIVED = "received";
    public static final String RESULT = "result";

    public static final String POLL = "poll";
    public static final String ADDRESS = "hostAddress";
    public static final String OWNER = "owner";
    public static final int OWN = 10;
    public static final int OTHER = 20;

    public static final String POLL_LIST = "poll_list";
    public static final String WAITING_LIST = "waiting_list";
    public static final String WAITING = "waiting";
    public static final int WAITED = 30;
    public static final String NOTIFICATION_ID = "notificationID";
    public static final String COUNT = "count";

}
