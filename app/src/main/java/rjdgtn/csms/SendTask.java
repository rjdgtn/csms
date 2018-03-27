package rjdgtn.csms;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Petr on 25.03.2018.
 */

public class SendTask implements Runnable {
    public BlockingQueue<short[]> inQueue;

    public SendTask() {
        inQueue = new LinkedBlockingQueue<short[]>();

        Log.d("CSMS", "SEND create");
    }

    boolean active = false;

    public void run() {
        int frequency = 8000;
        int channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;//CHANNEL_CONFIGURATION_MONO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

        int bufsize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

        AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC,
                frequency,
                channelConfiguration, //2 channel
                audioEncoding, // 16-bit
                bufsize * 2,
                AudioTrack.MODE_STREAM);

        audio.play();

        try {
            while (true) {
                short[] in = inQueue.take();
                if (in.length > 0) {
                    audio.write(in, 0, in.length);

                }
            }

        } catch (InterruptedException e) {
            audio.stop();
            audio.release();
            audio = null;
        }
    }
}
