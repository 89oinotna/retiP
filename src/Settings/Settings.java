package Settings;

public class Settings {
    public static final int RMIPort=8082;
    public static final int UDPPort=8081;
    public static final int TCPPort=8080;
    public static final int TCPBUFFLEN=1024;
    public static final String HOST_NAME="localhost";
    //todo fix parola e sfida

    /**                     COMANDI
     *
     * FORMATO COMANDI:     LOGIN NICK PW
     *                      LOGOUT NICK TOKEN
     *                      SFIDA NICK TOKEN FRIEND TYPE
     *                      AMICIZIA NICK TOKEN FRIEND TYPE
     *                      GET NICK TOKEN TYPE
     *                      PAROLA NICK TOKEN PAROLA
     */
    public enum REQUEST {
        LOGIN,
        LOGOUT,
        SFIDA,
        AMICIZIA,
        GET,
        PAROLA
    }
    public enum GetType {
        AMICI,
        CLASSIFICA,
        PENDING
    }
    public enum RQTType {
        RICHIEDI,
        ACCETTA,
        RIFIUTA
    }

    /**                     RISPOSTE
     *
     *  Tutte le risposte a un comando sono precedute da OK/NOK
     *  Le risposte asincrone no
     *
     *  FORMATO RISPOSTE:   OK AMICIZIA TOKEN FRIEND TYPE
     *                      OK SFIDA TOKEN NICK TYPE
     *                      OK PAROLA TOKEN NEXTWORD
     *                      OK TOKEN
     *                      NOK TOKEN ECCEZIONE
     *
     *  RISPOSTE INOLTRATE: AMICI TOKEN JSON
     *                      CLASSIFICA TOKEN JSON
     *                      PENDING TOKEN JSON
     *                      SFIDA NICK TOKEN (DA)FRIEND
     */
    public enum RESPONSE{
        LOGIN,
        AMICIZIA,
        AMICI,
        CLASSIFICA,
        PENDING,
        SFIDA,
        PAROLA,
        OK,
        NOK
    }
    public enum RSPType{
        RICHIESTA,
        ACCETTATA,
        RIFIUTATA
    }

    /**         CHALLENGE SETTINGS
     *
     */
    public static final int k=10; //numero di parole utilizzate per la sfida



    /**
     *          SFIDA
     */
    public static int TIMEOUT=10000; //ms
    public enum SFIDA {
        RICHIESTA,
        ACCETTATA,
        RIFIUTATA,
        INIZIATA,
        TERMINATA,
        SCADUTA
    }
    public static final long timer=60000;

    public static final int X=3; //Traduzione corretta assegna X punti
    public static final int Y=1; //Traduzione sbagliata toglie Y punti
    public static final int Z=5; //Chi vince ottiene Z punti extra
}
