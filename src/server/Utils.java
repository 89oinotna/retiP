package server;

public class Utils {
    public static String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            String hash = new String(array);
            return hash;
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }
}
