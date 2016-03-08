package freemap.opentrail03;

public interface AlertDisplay {
    public static final int ANNOTATION = 0, WALKROUTE_STAGE = 1;
    public void displayAnnotationInfo(String message, int type, int alertId);
}
