package Project.Common;

public class RemovingPlayerPayload extends Payload{
    private boolean isRemoved;

    public boolean isRemoved () {
        return isRemoved;
    }

    public void setRemoved(boolean isRemoved) {
        this.isRemoved = isRemoved;
    }

    public RemovingPlayerPayload () {
        setPayloadType(PayloadType.REMOVED);
    }
}
//zb64 4/18/24
