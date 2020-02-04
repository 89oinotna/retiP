package exceptions;

public class ChallengeException extends CustomException {
    public ChallengeException(String message) {
        super(message);
    }
    public ChallengeException(){
        super();
    }
}
