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
 * AAC encoder.
 * 
 * @author jiangzhen
 * 
 */
public class AACEncoder implements V2Encoder {

	private static final int SAMPLE_RATE = 44100;

	private static final int BIT_RATE = 64000;
	
	private static final int CHANNEL = 1;
	
	private static final int AAC_HEADER_LENGTH = 7;
	
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
	 * Current state
	 */
	private V2EncoderState mState = V2EncoderState.NORMAL;

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
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
			format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL);
			format.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
			format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
			format.setInteger(MediaFormat.KEY_AAC_PROFILE,
					MediaCodecInfo.CodecProfileLevel.AACObjectLC);
			mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		}
	}

	@Override
	public void start() {
		synchronized (mLock) {
			if (mIsRecording) {
				return;
			}
			mState = V2EncoderState.RECORDING;
			mIsRecording = true;
			mLocalRecorderThread.start();
		}

	}

	@Override
	public void stop() {
		synchronized (mLock) {
			mState = V2EncoderState.STOPPED;
			if (!mIsRecording) {
				return;
			}
			mIsRecording = false;
			mLocalRecorderThread.interrupt();
		}

	}
	
	
	

	@Override
	public V2EncoderState getState() {
		return mState;
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

				mEncoder.start();
				mRecorder.startRecording();
				try {
					writer = new FileOutputStream(mOutputFile);
				} catch (FileNotFoundException e) {
					mState = V2EncoderState.OUTPUT_ERROR;
					//FIXME set return error
					return;
				}
				while (mIsRecording) {
					read = mRecorder.read(audioBuffer, 0, mBufferSize);
					if (read <= 0) {
						mState = V2EncoderState.ERROR;
						mIsRecording = false;
						break;
					}
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
					outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo,
							0);

					while (outputBufferIndex >= 0) {
						outputBuffer = outputBuffers[outputBufferIndex];

						outputBuffer.position(bufferInfo.offset);
						outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

						outData = new byte[bufferInfo.size + AAC_HEADER_LENGTH];
						outputBuffer.get(outData, AAC_HEADER_LENGTH, outData.length - AAC_HEADER_LENGTH);

						
						fillAACHeader(outData);
						
						try {
							writer.write(outData, 0, outData.length);
						} catch (IOException e) {
							e.printStackTrace();
							//FIXME set error state
							mState = V2EncoderState.OUTPUT_ERROR;
							mIsRecording  = false;
							return;
						}
						
						mEncoder.releaseOutputBuffer(outputBufferIndex, false);
						outputBufferIndex = mEncoder.dequeueOutputBuffer(
								bufferInfo, 0);

					}

				}
				
				
				mEncoder.stop();
				mRecorder.stop();
				
				mState = V2EncoderState.NORMAL;
				
				try {
					writer.close();
				} catch (IOException e) {
					mState = V2EncoderState.OUTPUT_ERROR;
					e.printStackTrace();
				}

		}
		
		
		private void fillAACHeader(byte[] data) {
			if (data.length < AAC_HEADER_LENGTH) {
				throw new ArrayIndexOutOfBoundsException(" data length "+ data.length);
			}
			
			int profile = 2; // AAC LC
			int freqIdx = 4; // 44.1KHz
			int chanCfg = 2; // CPE

			// fill in ADTS data
			data[0] = (byte) 0xFF;
			data[1] = (byte) 0xF9;
			data[2] = (byte) (((profile - 1) << 6)
					+ (freqIdx << 2) + (chanCfg >> 2));
			data[3] = (byte) (((chanCfg & 3) << 6) + (data.length >> 11));
			data[4] = (byte) ((data.length & 0x7FF) >> 3);
			data[5] = (byte) (((data.length & 7) << 5) + 0x1F);
			data[6] = (byte) 0xFC;
		}

	};

}