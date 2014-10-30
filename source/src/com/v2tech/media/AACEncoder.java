package com.v2tech.media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder.AudioSource;
import android.os.Build;

/**
 * AAC encode and output to file or stream. <br>
 * FIXME only support android version greater than 4.04
 * @author jiangzhen
 * 
 */
public class AACEncoder implements V2Encoder {

	private static final int SAMPLE_RATE = 44100;

	private static final int BIT_RATE = 64000;

	private static final int CHANNEL = 1;

	private static final int AAC_HEADER_LENGTH = 7;
	
	private static final int MAX_INPUT_BUFFER_SIZE = 65536;

	private OutputStream writer;

	private File mOutputFile;

	/**
	 * Flag for current state
	 */
	private boolean mIsRecording;

	/**
	 * Lock for start or stop recording thread
	 */
	private Object mLock = new Object();

	/**
	 * AudioRecord for get audio raw data from hardware
	 */
	private AudioRecord mRecorder;

	/**
	 * MediaCodec for encode AAC packet
	 */
	private MediaCodec mEncoder;

	/**
	 * Buffer size for minimal audio buffer size
	 */
	private int mBufferSize;
	
	/**
	 * record current frame's db
	 */
	private double mDB;

	/**
	 * Current state
	 */
	private MediaState mState = MediaState.NORMAL;
	
	
	private ErrorCallback mLocalCallback;

	/**
	 * 
	 * @param file
	 */
	public AACEncoder(File file) {
		if (file == null) {
			throw new NullPointerException(" file is null");
		}
		mOutputFile = file;
		initEncoder();
	}

	public AACEncoder(String filePath) {
		if (filePath == null) {
			throw new NullPointerException(" file is null");
		}
		mOutputFile = new File(filePath);
		initEncoder();
	}

	/**
	 * 
	 * @param out
	 */
	public AACEncoder(OutputStream out) {
		if (out == null) {
			throw new NullPointerException(" file is null");
		}

		writer = out;
		initEncoder();
	}

	private void initEncoder() {
		mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		if (mBufferSize == AudioRecord.ERROR_BAD_VALUE) {
			throw new RuntimeException(
					" Can not initialze AudioRecord because buffer size is bad value");
		}
		mRecorder = new AudioRecord(AudioSource.MIC, SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
				mBufferSize);
 
		mEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
		MediaFormat format = new MediaFormat();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
			format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL);
			format.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
			format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
			format.setInteger(MediaFormat.KEY_AAC_PROFILE,
					MediaCodecInfo.CodecProfileLevel.AACObjectLC);
			format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_BUFFER_SIZE);
			mEncoder.configure(format, null, null,
					MediaCodec.CONFIGURE_FLAG_ENCODE);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			throw new RuntimeException(" Doesn't support on lower 4.1 version");
		} else {
			throw new RuntimeException(" Doesn't support on lower 4.1 version");
		}
	}

	@Override
	public void start() {
		synchronized (mLock) {
			if (mIsRecording) {
				return;
			}
			
			if (mState == MediaState.RELEASED) {
				throw new RuntimeException("encoder has released!");
			}
			mState = MediaState.RECORDING;
			mIsRecording = true;
			mLocalRecorderThread.start();
		}

	}

	@Override
	public void stop() {
		synchronized (mLock) {
			updateLocalState(MediaState.STOPPED);
			if (!mIsRecording) {
				return;
			}
			mIsRecording = false;
			mLocalRecorderThread.interrupt();
		}

	}
	
	
	

	@Override
	public void release() {
		if (mEncoder != null) {
			mEncoder.release();
		}
		
		if (mRecorder != null) {
			mRecorder.release();
		}
		synchronized (mLock) {
			mIsRecording = false ;
			updateLocalState(MediaState.RELEASED);
		}
	}

	@Override
	public MediaState getState() {
		return mState;
	}
	
	
	@Override
	public double getDB() {
		return mDB;
	}
	
	
	

	@Override
	public void setErrorCallback(ErrorCallback callback) {
		this.mLocalCallback = callback;
	}


	
	private void updateLocalState(MediaState state) {
		mState = state;
		if (mLocalCallback != null && (state == MediaState.ERROR || state == MediaState.OUTPUT_ERROR)) {
			mLocalCallback.onError(state);
		}
	}


	private Thread mLocalRecorderThread = new Thread() {

		@Override
		public void run() {
			int read;
			byte[] audioBuffer = new byte[mBufferSize];

			ByteBuffer[] inputBuffers;
			ByteBuffer[] outputBuffers;

			ByteBuffer inputBuffer;
			ByteBuffer outputBuffer;

			MediaCodec.BufferInfo bufferInfo;
			int inputBufferIndex;
			int outputBufferIndex;

			byte[] outData;

			try {
				writer = new FileOutputStream(mOutputFile);
			} catch (FileNotFoundException e) {
				updateLocalState(MediaState.OUTPUT_ERROR);
				return;
			}

			mEncoder.start();
			mRecorder.startRecording();
			while (mIsRecording) {
				read = mRecorder.read(audioBuffer, 0, mBufferSize);
				if (read <= 0) {
					updateLocalState(MediaState.ERROR);
					mIsRecording = false;
					break;
				}
				
				saveDB(audioBuffer);
				
				inputBuffers = mEncoder.getInputBuffers();
				outputBuffers = mEncoder.getOutputBuffers();
				inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
				if (inputBufferIndex >= 0) {
					inputBuffer = inputBuffers[inputBufferIndex];
					inputBuffer.clear();
					inputBuffer.put(audioBuffer);

					mEncoder.queueInputBuffer(inputBufferIndex, 0,
							audioBuffer.length, 0, 0);
				}

				bufferInfo = new MediaCodec.BufferInfo();
				outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, 0);

				while (outputBufferIndex >= 0) {
					outputBuffer = outputBuffers[outputBufferIndex];

					outputBuffer.position(bufferInfo.offset);
					outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

					outData = new byte[bufferInfo.size + AAC_HEADER_LENGTH];
					outputBuffer.get(outData, AAC_HEADER_LENGTH, outData.length
							- AAC_HEADER_LENGTH);

					fillAACHeader(outData);

					try {
						writer.write(outData, 0, outData.length);
					} catch (IOException e) {
						e.printStackTrace();
						updateLocalState(MediaState.OUTPUT_ERROR);
						mIsRecording = false;
						break;
					}

					mEncoder.releaseOutputBuffer(outputBufferIndex, false);
					outputBufferIndex = mEncoder.dequeueOutputBuffer(
							bufferInfo, 0);

				}

			}

			mEncoder.stop();
			mRecorder.stop();

			updateLocalState(MediaState.NORMAL);

			try {
				writer.close();
			} catch (IOException e) {
				updateLocalState(MediaState.OUTPUT_ERROR);
				e.printStackTrace();
			}

		}

		private void saveDB(byte[] audiobuffer) {
			 int amplitude = (audiobuffer[0] & 0xff) << 8 | audiobuffer[1];
			 
			 mDB = 20 * Math.log10((double)Math.abs(amplitude) / 32768);
		}
		
		
		private void fillAACHeader(byte[] data) {
			if (data.length < AAC_HEADER_LENGTH) {
				throw new ArrayIndexOutOfBoundsException(" data length "
						+ data.length);
			}

			int profile = 2; // AAC LC
			int freqIdx = 4; // 44.1KHz
			int chanCfg = 2; // CPE

			// fill in ADTS data
			data[0] = (byte) 0xFF;
			data[1] = (byte) 0xF9;
			data[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
			data[3] = (byte) (((chanCfg & 3) << 6) + (data.length >> 11));
			data[4] = (byte) ((data.length & 0x7FF) >> 3);
			data[5] = (byte) (((data.length & 7) << 5) + 0x1F);
			data[6] = (byte) 0xFC;
		}

	};

}
