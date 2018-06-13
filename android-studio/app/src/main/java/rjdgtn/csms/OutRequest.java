package rjdgtn.csms;

/**
 * Created by Petr on 25.03.2018.
 */

public class OutRequest {
    OutRequest(byte[] data) {
        this.data = data;
    }
    OutRequest(String request) { this.request = request; }
    OutRequest(String request, int intParam) {
        this.request = request;
        this.intParam = intParam;
    }
    public byte[] data = null;
    public String request = null;
    public int intParam = 0;
};
