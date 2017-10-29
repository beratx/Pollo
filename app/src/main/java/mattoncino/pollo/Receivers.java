package mattoncino.pollo;

/**
 * Constant strings used for/by broadcast listeners
 */
public final class Receivers {
    public static final String WIFI = "mattoncino.pollo.receive.wifi.stat";
    public static final String ACCEPT = "mattoncino.pollo.receive.poll.accept";
    public static final String VOTE  = "mattoncino.pollo.receive.poll.vote";
    public static final String RESULT  = "mattoncino.pollo.receive.poll.result";
    public static final String REMOVE  = "mattoncino.pollo.receive.poll.remove";
    public static final String W_ADD  = "mattoncino.pollo.receive.waiting.add";
    public static final String W_REMOVE  = "mattoncino.pollo.receive.waiting.remove";
    public static final String W_COUNT = "mattoncino.pollo.receive.waiting.count";

}
