package de.showlabor.example.pitchplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Felix Homann, linuxaudio@showlabor.de
 *
 * This is a quick and dirty demo of how to use MediaExtractor, MediaCodec and AudioTrack
 * to create a pitch shifting audio stream player.
 *
 * Nothing here seems to be copyrightable IMO.
 *
 * NOTE TO MYSELF: DON'T FORGET TO ADD INTERNET PERMISSION TO THE APP!
 *
 */
public class PitchPlayer implements Runnable {

    private MediaCodec mCodec;
    private MediaExtractor mExtractor;
    private String mPath;
    private AudioTrack mAudioTrack = null;
    private int mBufferSize = 0;

    private float mRelativePlaybackSpeed = 1.f;
    private int mSrcRate = 44100;
    private boolean isPlaying = false;
    private boolean doStop = false;

    private final static long TIMEOUT_US = 100000;

    public PitchPlayer(String path) {
        mPath = path;
    }

    private void prepare() {
        mExtractor = new MediaExtractor();

        try {
            mExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mExtractor.getTrackCount() > 0) {
            // Get mime type of the first track
            MediaFormat format = mExtractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio")) {
                mCodec = MediaCodec.createDecoderByType(mime);
                mCodec.configure(format,
                        null, // We don't have a surface in audio decoding
                        null, // No crypto
                        0); // 0 for decoding

                // Select the first track for decoding
                mExtractor.selectTrack(0);
                mCodec.start(); // Fire up the codec
                //Don't make the buffer size too small:
                mBufferSize = 8 * AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        44100,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        mBufferSize,
                        AudioTrack.MODE_STREAM);
                mAudioTrack.play();
            }
        }
    }

    public void start() {
        isPlaying = true;
        doStop = false;
        new Thread(this).start();
    }

    public void stop() {
        doStop = true;
    }

    public void run() {
        prepare();
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        ByteBuffer[] outBuffers = mCodec.getOutputBuffers();
        ByteBuffer activeOutBuffer = null; // The active output buffer
        int activeIndex = 0; // Index of hte active buffer

        int availableOutBytes = 0;
        int writeableBytes = 0;
        final byte[] writeBuffer = new byte[mBufferSize];
        int writeOffset = 0;

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        boolean EOS = false;

        while (!Thread.interrupted() && !doStop) {
            if (!EOS) {
                // Dequeue an input buffer
                int inIndex = mCodec.dequeueInputBuffer(TIMEOUT_US);
                if (inIndex >= 0) {
                    ByteBuffer buffer = inputBuffers[inIndex];
                    int sampleSize = mExtractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        // We have reached the end of the stream
                        mCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        EOS = true;
                    } else {
                        mCodec.queueInputBuffer(inIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);
                        mExtractor.advance();
                    }
                }
            }

            if (availableOutBytes == 0) {
                activeIndex = mCodec.dequeueOutputBuffer(info, TIMEOUT_US);

                // outIndex might carry some information for us.
                switch (activeIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    outBuffers = mCodec.getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    MediaFormat outFormat = mCodec.getOutputFormat();
                    mSrcRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    mAudioTrack.setPlaybackRate((int) (mSrcRate * mRelativePlaybackSpeed));
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    // Nothing to do
                    break;
                default:
                    activeOutBuffer = outBuffers[activeIndex];
                    availableOutBytes = info.size;
                    assert info.offset == 0;
                }
            }

            if (activeOutBuffer != null && availableOutBytes > 0) {
                writeableBytes = Math.min(availableOutBytes, mBufferSize - writeOffset);
                // Copy samples to writeBuffer
                activeOutBuffer.get(writeBuffer, writeOffset, writeableBytes);
                availableOutBytes -= writeableBytes;
                writeOffset += writeableBytes;
            }

            if (writeOffset == mBufferSize) {
                // The buffer is full. Submit it to the AudioTrack
                mAudioTrack.write(writeBuffer, 0, mBufferSize);
                writeOffset = 0;
            }

            if (activeOutBuffer != null && availableOutBytes == 0) {
                // IMPORTANT: Clear the active buffer!
                activeOutBuffer.clear();
                if (activeIndex >= 0) {
                    mCodec.releaseOutputBuffer(activeIndex, false);
                }
            }

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                // Get out of here
                break;
            }
        }

        //Clean up
        isPlaying = false;
        doStop = false;
        mCodec.stop();
        mCodec.release();
        mExtractor.release();
    }

    public void setRelativePlaybackSpeed(float speed) {
        mRelativePlaybackSpeed = speed;
        if (mAudioTrack != null) {
            mAudioTrack.setPlaybackRate((int) (mSrcRate * mRelativePlaybackSpeed));
        }
    }

    public void setVolume(float vol) {
        if (mAudioTrack != null) {
            mAudioTrack.setStereoVolume(vol, vol);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
