package com.limelight.browser;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.view.Surface;
import android.view.View;

import com.limelight.LimeLog;

import java.io.File;
import java.nio.ByteBuffer;

/** Phase-1 stream prep: browser view + mic to local MP4 (H.264/AAC). */
public final class StreamRecorder {
    private static final int W = 854, H = 480, FPS = 15, SECONDS = 12;
    private static final int VBR = 2_500_000, ABR = 128_000, SR = 44100;
    private final View source;
    private volatile boolean running;
    private volatile int audioPeak;
    private volatile boolean queueLogged;
    private final long[] basePts = {-1, -1, -1, -1};

    public StreamRecorder(View source) { this.source = source; }

    public void startAsync(File out) {
        running = true;
        new Thread(() -> record(out), "StreamRecorder").start();
    }

    private void record(File out) {
        MediaCodec video = null, audio = null;
        MediaMuxer muxer = null;
        AudioRecord mic = null;
        try {
            MediaFormat vf = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC, W, H);
            vf.setInteger(MediaFormat.KEY_BIT_RATE, VBR);
            vf.setInteger(MediaFormat.KEY_FRAME_RATE, FPS);
            vf.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
            vf.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            video = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            video.configure(vf, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface input = video.createInputSurface();
            video.start();

            MediaFormat af = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC, SR, 1);
            af.setInteger(MediaFormat.KEY_BIT_RATE, ABR);
            af.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            af.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
            audio = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            audio.configure(af, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            audio.start();

            int minBuf = AudioRecord.getMinBufferSize(SR,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mic = new AudioRecord(MediaRecorder.AudioSource.MIC, SR,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(minBuf, 8192));
            mic.startRecording();

            muxer = new MediaMuxer(out.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaCodec.BufferInfo vInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo aInfo = new MediaCodec.BufferInfo();
            Bitmap frame = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
            long t0 = System.nanoTime();

            // Prime both encoders until formats are known, then start muxer.
            int vTrack = -1, aTrack = -1;
            long primeEnd = t0 + 3_000_000_000L;
            while ((vTrack < 0 || aTrack < 0) && System.nanoTime() < primeEnd) {
                drawFrame(input, frame);
                feedAudio(audio, mic, t0);
                if (vTrack < 0) vTrack = tryFormat(video, vInfo, muxer);
                if (aTrack < 0) aTrack = tryFormat(audio, aInfo, muxer);
                Thread.sleep(1000 / FPS);
            }
            if (vTrack < 0 || aTrack < 0) {
                LimeLog.warning("BrowserDiag REC no format v=" + vTrack + " a=" + aTrack);
                return;
            }
            muxer.start();
            long endNs = t0 + SECONDS * 1_000_000_000L;
            int frames = 0;
            while (System.nanoTime() < endNs && running) {
                drawFrame(input, frame);
                feedAudio(audio, mic, t0);
                drain(video, vInfo, muxer, vTrack);
                drain(audio, aInfo, muxer, aTrack);
                frames++;
                Thread.sleep(1000 / FPS);
            }
            video.signalEndOfInputStream();
            int eosIdx = audio.dequeueInputBuffer(10000);
            if (eosIdx >= 0) {
                audio.queueInputBuffer(eosIdx, 0, 0,
                        (System.nanoTime() - t0) / 1000,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }
            drainEos(video, vInfo, muxer, vTrack);
            drainEos(audio, aInfo, muxer, aTrack);
            muxer.stop();
            LimeLog.info("BrowserDiag REC done file=" + out.getName()
                    + " bytes=" + out.length() + " frames~" + frames
                    + " audioPeak=" + audioPeak);
        } catch (Exception e) {
            LimeLog.warning("BrowserDiag REC fail " + e);
        } finally {
            try { if (mic != null) mic.release(); } catch (Exception ignored) {}
            try { if (video != null) video.release(); } catch (Exception ignored) {}
            try { if (audio != null) audio.release(); } catch (Exception ignored) {}
            try { if (muxer != null) muxer.release(); } catch (Exception ignored) {}
        }
    }

    private int tryFormat(MediaCodec codec, MediaCodec.BufferInfo info,
                          MediaMuxer muxer) {
        int idx = codec.dequeueOutputBuffer(info, 0);
        if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            return muxer.addTrack(codec.getOutputFormat());
        }
        if (idx >= 0) codec.releaseOutputBuffer(idx, false);
        return -1;
    }

    private void feedAudio(MediaCodec audio, AudioRecord mic, long t0) {
        try {
            byte[] pcm = new byte[2048];
            int read = mic.read(pcm, 0, pcm.length);
            if (read <= 0) return;
            for (int i = 0; i + 1 < read; i += 2) {
                int s = Math.abs((pcm[i] & 255) | (pcm[i + 1] << 8));
                if (s > audioPeak) audioPeak = s;
            }
            int idx = audio.dequeueInputBuffer(0);
            if (idx < 0) return;
            ByteBuffer buf = audio.getInputBuffer(idx);
            buf.clear();
            buf.put(pcm, 0, read);
            audio.queueInputBuffer(idx, 0, read,
                    (System.nanoTime() - t0) / 1000, 0);
        } catch (Exception ignored) {}
    }

    private void drawFrame(Surface input, Bitmap frame) {
        drawView(frame);
        try {
            Canvas sc = input.lockCanvas(null);
            sc.drawBitmap(frame, null, new Rect(0, 0, W, H), null);
            input.unlockCanvasAndPost(sc);
        } catch (Exception ignored) {}
    }

    private void drawView(Bitmap frame) {
        try {
            source.post(() -> {
                try {
                    Canvas c = new Canvas(frame);
                    c.scale((float) W / Math.max(1, source.getWidth()),
                            (float) H / Math.max(1, source.getHeight()));
                    source.draw(c);
                } catch (Exception ignored) {}
            });
            Thread.sleep(12);
        } catch (Exception ignored) {}
    }

    private final int[] argb = new int[W * H];
    private byte[] yuv = new byte[W * H * 3 / 2];
    private int encStride = W, encSlice = H;

    private void queueVideo(MediaCodec video, Bitmap frame, long t0) {
        try {
            int idx = video.dequeueInputBuffer(0);
            if (idx < 0) return;
            frame.getPixels(argb, 0, W, 0, 0, W, H);
            android.media.Image img = video.getInputImage(idx);
            if (img == null) return;
            android.media.Image.Plane[] planes = img.getPlanes();
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    int px = argb[y * W + x];
                    int r = (px >> 16) & 255, g = (px >> 8) & 255, b = px & 255;
                    put(planes[0], x, y,
                            clamp(((66 * r + 129 * g + 25 * b + 128) >> 8) + 16));
                    if ((y & 1) == 0 && (x & 1) == 0) {
                        put(planes[1], x / 2, y / 2,
                                clamp(((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128));
                        put(planes[2], x / 2, y / 2,
                                clamp(((112 * r - 94 * g - 18 * b + 128) >> 8) + 128));
                    }
                }
            }
            video.queueInputBuffer(idx, 0, 0,
                    (System.nanoTime() - t0) / 1000, 0);
        } catch (Exception e) {
            if (!queueLogged) {
                queueLogged = true;
                LimeLog.warning("BrowserDiag REC queueVideo fail " + e);
            }
        }
    }

    private static int clamp(int v) { return v < 0 ? 0 : Math.min(255, v); }

    private static void put(android.media.Image.Plane plane, int x, int y, int v) {
        ByteBuffer b = plane.getBuffer();
        int pos = y * plane.getRowStride() + x * plane.getPixelStride();
        if (pos >= 0 && pos < b.capacity()) b.put(pos, (byte) v);
    }

    private void drain(MediaCodec codec, MediaCodec.BufferInfo info,
                       MediaMuxer muxer, int track) {
        while (true) {
            int idx = codec.dequeueOutputBuffer(info, 0);
            if (idx < 0) return;
            write(codec, info, muxer, track, idx);
        }
    }

    private void drainEos(MediaCodec codec, MediaCodec.BufferInfo info,
                          MediaMuxer muxer, int track) {
        while (true) {
            int idx = codec.dequeueOutputBuffer(info, 10000);
            if (idx < 0) return;
            write(codec, info, muxer, track, idx);
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return;
        }
    }

    private void write(MediaCodec codec, MediaCodec.BufferInfo info,
                       MediaMuxer muxer, int track, int idx) {
        ByteBuffer buf = codec.getOutputBuffer(idx);
        if (buf != null && info.size > 0
                && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
            if (basePts[track] < 0) basePts[track] = info.presentationTimeUs;
            MediaCodec.BufferInfo adj = new MediaCodec.BufferInfo();
            adj.set(info.offset, info.size,
                    Math.max(0, info.presentationTimeUs - basePts[track]), info.flags);
            buf.position(info.offset);
            buf.limit(info.offset + info.size);
            try { muxer.writeSampleData(track, buf, adj); }
            catch (Exception ignored) {}
        }
        codec.releaseOutputBuffer(idx, false);
    }
}
