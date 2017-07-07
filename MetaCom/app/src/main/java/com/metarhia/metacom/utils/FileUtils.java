package com.metarhia.metacom.utils;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import com.metarhia.metacom.connection.Errors;
import com.metarhia.metacom.connection.JSTPOkErrorHandler;
import com.metarhia.metacom.interfaces.FileDownloadedCallback;
import com.metarhia.metacom.interfaces.FileUploadedCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utils for files manipulations
 *
 * @author lidaamber
 */

public class FileUtils {

    private static final String FILE_HANDLER_THREAD = "fileHandlerThread";

    static {
        HandlerThread fileHandlerThread = new HandlerThread(FILE_HANDLER_THREAD);
        fileHandlerThread.start();
        sFileHandler = new Handler(fileHandlerThread.getLooper());
    }

    /**
     * Handler processing files manipulations
     */
    private static Handler sFileHandler;

    /**
     * Size of chunk to split file
     */
    private static final int FILE_CHUNK_SIZE = 1 * 1024 * 1024;

    /**
     * Uploads file to server
     *
     * @param is            file to upload
     * @param sendInterface send and end sending methods specific for uploading
     * @param callback      callback after file upload (success and error)
     */
    public static void uploadSplitFile(InputStream is, final FileUploadingInterface sendInterface,
                                       final FileUploadedCallback callback) {
        splitFile(is, FILE_CHUNK_SIZE, new FileUtils.FileContentsCallback() {
            @Override
            public void onSplitToChunks(List<byte[]> chunks) {
                final Iterator<byte[]> chunkIterator = chunks.iterator();
                if (chunkIterator.hasNext()) {
                    byte[] chunk = chunkIterator.next();
                    final JSTPOkErrorHandler handler = new JSTPOkErrorHandler(MainExecutor.get()) {
                        @Override
                        public void onOk(List<?> args) {
                            if (chunkIterator.hasNext()) {
                                sendInterface.sendChunk(chunkIterator.next(), this);
                            } else {
                                sendInterface.endFileUpload(callback);
                            }
                        }

                        @Override
                        public void onError(Integer errorCode) {
                            callback.onFileUploadError(Errors.getErrorByCode(errorCode));
                        }
                    };
                    sendInterface.sendChunk(chunk, handler);
                }
            }

            @Override
            public void onSplitError(Exception e) {
                callback.onFileUploadError(Errors.getErrorByCode(Errors.ERR_FILE_LOAD));
            }
        });
    }

    /**
     * Splits file into byte[] chunks
     *
     * @param fileStream file to split
     * @param chunkSize  size of the chunk
     * @param callback   callback on file split
     */
    private static void splitFile(final InputStream fileStream, final int chunkSize,
                                  final FileContentsCallback callback) {
        sFileHandler.post(new Runnable() {
            @Override
            public void run() {
                List<byte[]> chunks = new ArrayList<>();
                try {
                    final byte[] buf = new byte[chunkSize];
                    while (fileStream.read(buf) != -1) {
                        chunks.add(buf);
                    }
                    fileStream.close();

                    callback.onSplitToChunks(chunks);
                } catch (FileNotFoundException e) {
                    callback.onSplitError(e);
                } catch (IOException e) {
                    callback.onSplitError(e);
                }
            }
        });
    }

    /**
     * Gets downloads storage
     *
     * @return downloads storage
     */
    public static void saveFileInDownloads(String extension, ArrayList<byte[]> buffer,
                                           final FileDownloadedCallback callback) {
        try {
            final File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), System.currentTimeMillis() + "." + extension);
            file.createNewFile();

            FileUtils.writeChunksToFile(file, buffer,
                    new FileUtils.FileWritingCallback() {
                        @Override
                        public void onWrittenToFile() {
                            MainExecutor.get().execute(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFileDownloaded(file.getAbsolutePath());
                                }
                            });
                        }

                        @Override
                        public void onWriteError(Exception e) {
                            callback.onFileDownloadError();
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
            callback.onFileDownloadError();
        }
    }

    /**
     * Writes file chunks to file
     *
     * @param file   file to be written
     * @param chunks file chunks
     */
    public static void writeChunksToFile(final File file, final ArrayList<byte[]> chunks,
                                         final FileWritingCallback callback) {
        sFileHandler.post(new Runnable() {
            @Override
            public void run() {
                FileOutputStream stream;
                try {
                    stream = new FileOutputStream(file);
                    for (byte[] chunk : chunks) {
                        stream.write(chunk);
                    }
                    stream.flush();
                    stream.close();

                    callback.onWrittenToFile();

                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onWriteError(e);
                }
            }
        });
    }

    /**
     * Callback for splitting files
     */
    private interface FileContentsCallback {
        /**
         * Called when file is split successfully
         *
         * @param chunks file chunks
         */
        void onSplitToChunks(List<byte[]> chunks);

        /**
         * Called when splitting fails
         *
         * @param e exception thrown while splitting
         */
        void onSplitError(Exception e);
    }

    /**
     * Callback for writing into file
     */
    public interface FileWritingCallback {

        /**
         * Called when content was written successfully
         */
        void onWrittenToFile();

        /**
         * Called when writing failed
         *
         * @param e exception thrown while writing
         */
        void onWriteError(Exception e);
    }

    /**
     * Interface used to describe file uploading
     */
    public interface FileUploadingInterface {
        void sendChunk(byte[] chunk, JSTPOkErrorHandler handler);

        void endFileUpload(FileUploadedCallback callback);
    }
}
