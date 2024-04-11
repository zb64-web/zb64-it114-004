package Project.Common;
import java.io.Serializable;

public class Payload implements Serializable{
    private long clientId;
    private String playerChoice;

    public Payload() {
        //TODO Auto-generated constructor stub
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getPlayerChoice() {
        return playerChoice;
    }

    public void setPlayerChice(String playerChoice) {
        this.playerChoice = playerChoice;
    }
    //zb64 4/9/24

    // read https://www.baeldung.com/java-serial-version-uid
    private static final long serialVersionUID = 1L;// change this if the class changes

    /**
     * Determines how to process the data on the receiver's side
     */
    private PayloadType payloadType;

    public PayloadType getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(PayloadType payloadType) {
        this.payloadType = payloadType;
    }



    /**
     * Generic text based message
     */
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    @Override
    public String toString() {
        return String.format("Type[%s], Message[%s], ClientId[%s]", getPayloadType().toString(),
                getMessage(), getClientId());
    }
}
