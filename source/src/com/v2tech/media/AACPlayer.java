package com.v2tech.media;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

public class AACPlayer implements V2Decoder {

	/**
	 * Current samlpe rate which parse from audio resource
	 */
	private int mSampleRate;

	/**
	 * Current channel configuration e which parse from audio resource<br>
	 * AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO;
	 */
	private int mChannel;

	/**
	 * Current Audio Tracker supported minimal buffer size
	 */
	private int mMinBufferSize;

	/**
	 * Audio resource duration which parse from audio resource
	 */
	private long mDurationUS;

	/**
	 * End of audio resource
	 */
	private boolean mEOF;

	private MediaExtractor mExtractor;

	private MediaCodec mDecoder;

	private AudioTrack mAudioTracker;

	private MediaState mState = MediaState.NORMAL;

	/**
	 * Local error callback
	 */
	private ErrorCallback mLocalErrorCalback;

	private String mFilePath;

	/**
	 * local lock
	 */
	private Object mLock = new Object();

	/**
	 * 
	 * @param filePath
	 *            file path can not be null
	 */
	public AACPlayer(String filePath) {
		this(filePath, null);
	}

	/**
	 * 
	 * @param filePath
	 *            file path can not be null
	 * @param callback
	 */
	public AACPlayer(String filePath, ErrorCallback callback) {
		this.mFilePath = filePath;
		this.mLocalErrorCalback = callback;
		if (filePath == null) {
			throw new NullPointerException(" file path can not be null");
		}

		initDecoder();
	}

	private void initDecoder() {

		mExtractor = new MediaExtractor();
		try {
			mExtractor.setDataSource(mFilePath);
		} catch (IOException e) {
			e.printStackTrace();
			updateDecodeState(MediaState.DECODE_ERROR);
			return;
		}

		int trackNum = mExtractor.getTrackCount();
		if (trackNum <= 0) {
			updateDecodeState(MediaState.DECODE_ERROR);
			return;
		}

		String mime = "audio/mp4a-latm";

		// Only support first audio format
		MediaFormat format = mExtractor.getTrackFormat(0);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			mime = format.getString(MediaFormat.KEY_MIME);
			// Get sample rate
			mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
			// Get channel count
			int channel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
			mChannel = channel <= 2 ? AudioFormat.CHANNEL_IN_MONO
					: AudioFormat.CHANNEL_IN_STEREO;
			// Get duration of audio
			mDurationUS = format.getInteger(MediaFormat.KEY_DURATION);

		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			throw new RuntimeException(" Doesn't support on lower 4.1 version");
		} else {
			throw new RuntimeException(" Doesn't support on lower 4.1 version");
		}

		// FIXME always use ENCODING_PCM_16BIT
		mMinBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannel,
				AudioFormat.ENCODING_PCM_16BIT);
		if (mMinBufferSize == AudioTrack.ERROR_BAD_VALUE) {
			throw new RuntimeException(
					" Doesn't support this configuration : [" + mSampleRate
							+ ", " + mChannel + ", "
							+ AudioFormat.ENCODING_PCM_16BIT + "]");
		}

		mDecoder = MediaCodec.createDecoderByType(mime);
		mDecoder.configure(format, null, null, 0);

		mAudioTracker = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate,
				mChannel, AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize,
				AudioTrack.MODE_STREAM);

		// Always switch to first format
		mExtractor.selectTrack(0);

	}

	@Override
	public synchronized void play() {
		updateDecodeState(MediaState.DECODING);
		mEOF = false;
		if (!mDecoderWorker.isAlive()) {
			mDecoderWorker.start();
		} else {
			updateDecodeState(MediaState.DECODING);
			synchronized (mLock) {
				mState.notify();
			}
		}

	}

	@Override
	public void stop() {
		mEOF = true;
		updateDecodeState(MediaState.STOPPED);
	}

	@Override
	public void seek(int sec) {
		if (sec * 1000000 > this.mDurationUS) {
			throw new RuntimeException(" Seconds out of duration" + getDuration());
		}
		 mExtractor.seekTo(sec * 1000000 , MediaExtractor.SEEK_TO_CLOSEST_SYNC);

	}

	@Override
	public void release() {
		mDecoder.release();
		mAudioTracker.release();
		mExtractor.release();
		updateDecodeState(MediaState.RELEASED);

	}

	@Override
	public int getDuration() {
		return (int) mDurationUS / 1000000;
	}

	@Override
	public void pause() {
		updateDecodeState(MediaState.PAUSE);
		synchronized (mLock) {
			mState.notify();
		}
	}

	@Override
	public void setErrorCallback(ErrorCallback callback) {
		mLocalErrorCalback = callback;

	}

	private void updateDecodeState(MediaState newState) {
		synchronized (mLock) {
			mState = newState;
			if (mLocalErrorCalback != null
					&& (newState == MediaState.DECODE_ERROR || newState == MediaState.DECODE_ERROR)) {
				mLocalErrorCalback.onError(newState);
			}
		}
	}

	private Thread mDecoderWorker = new Thread() {

		@Override
		public void run() {
			mDecoder.start();
			mAudioTracker.play();

			ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
			ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();

			int inputBufferIndex = -1;
			int readSize = -1;
			long presentationTimeUs;

			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

			while (!mEOF) {

				if (mState == MediaState.PAUSE) {
					try {
						mState.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else if (mState == MediaState.STOPPED) {
					mEOF = true;
					break;
				}

				inputBufferIndex = mDecoder.dequeueInputBuffer(1000);
				if (inputBufferIndex >= 0) {
					ByteBuffer buffer = inputBuffers[inputBufferIndex];
					buffer.clear();
					readSize = mExtractor.readSampleData(buffer, 0);
					if (readSize > 0) {
						presentationTimeUs = mExtractor.getSampleTime();
						mDecoder.queueInputBuffer(inputBufferIndex, 0,
								readSize, presentationTimeUs, 0);

					} else {
						mDecoder.queueInputBuffer(inputBufferIndex, 0, 0, 0,
								MediaCodec.BUFFER_FLAG_END_OF_STREAM);
					}
					mExtractor.advance();
				}

				int outputBufferIndex = mDecoder.dequeueOutputBuffer(
						bufferInfo, 1000);
				if (outputBufferIndex >= 0) {
					ByteBuffer buffer = outputBuffers[outputBufferIndex];
					byte[] data = new byte[bufferInfo.size];
					buffer.get(data);
					buffer.clear();
					// Flush audio raw data to hardware
					mAudioTracker.write(data, 0, data.length);

					// outputBuffer is ready to be processed or
					// rendered.
					mDecoder.releaseOutputBuffer(outputBufferIndex, false);

					if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
						mEOF = true;
						//Update state to stop
						updateDecodeState(MediaState.STOPPED);
						break;
					}
				} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					// no needed to handle if API level >= 21 and
					// using getOutputBuffer(int)
					outputBuffers = mDecoder.getOutputBuffers();
				} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					// Subsequent data will conform to new format.
					// can ignore if API level >= 21 and using
					// getOutputFormat(outputBufferIndex)
					// format = mDecoder.getOutputFormat();
				}

			}
			
			mDecoder.stop();
			mAudioTracker.stop();
		}

	};

}
