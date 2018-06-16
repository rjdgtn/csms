package rjdgtn.csms;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class Sms {

    private final String ENCODING = "UTF-8";

    short id = 0;
    byte kind = 0;
    int date = 0;
    String number = null;
    String text = null;

    Sms () { }

    Sms (byte kind, ByteBuffer buffer) throws UnsupportedEncodingException {
      id = buffer.getShort();
      this.kind = kind;
      date = buffer.getInt();

      byte[] numberBuf = new byte[buffer.get()];
      buffer.get(numberBuf);
      number = new String(numberBuf, ENCODING);

      byte[] textBuf = new byte[buffer.remaining()];
      buffer.get(textBuf);
      text = new String(textBuf, ENCODING);
    }

    byte[] toBytes() throws UnsupportedEncodingException {
        ByteBuffer buffer = ByteBuffer.allocate(
                2 + 4 + 1 + number.getBytes(ENCODING).length + 2 + text.getBytes(ENCODING).length
        );

        buffer.putShort(id);
        buffer.putInt(date);
        buffer.put((byte)number.length());
        buffer.put(number.getBytes(ENCODING));
        buffer.put(text.getBytes(ENCODING));

        return buffer.array();
    }
}
