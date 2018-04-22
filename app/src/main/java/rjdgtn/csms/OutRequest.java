package rjdgtn.csms;

/**
 * Created by Petr on 25.03.2018.
 */

public class OutRequest {
    OutRequest(byte[] data) {
        this.data = data;
    }
    OutRequest(String request) {
        this.request = request;
    }
    public byte[] data = null;
    public String request = null;
};
