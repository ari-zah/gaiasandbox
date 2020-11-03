/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.util.Logger.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * Contains utilities to download files
 *
 * @author tsagrista
 */
public class DownloadHelper {
    private static final Log logger = Logger.getLogger(DownloadHelper.class);

    /*
     * Spawns a new thread which downloads the file from the given location, running the
     * progress {@link ProgressRunnable} while downloading, and running the finish {@link java.lang.Runnable}
     * when finished.
     */
    public static HttpRequest downloadFile(String url, FileHandle to, ProgressRunnable progress, ChecksumRunnable finish, Runnable fail, Runnable cancel) {

        if (url.startsWith("file://")) {
            // Local file!
            String srcString = url.replaceFirst("file://", "");
            Path source = Paths.get(srcString);
            logger.info("Using file:// protocol: " + srcString);
            if (Files.exists(source) && Files.isRegularFile(source) && Files.isReadable(source)) {
                Path target = Path.of(to.path());
                try {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    // Run finish with empty digest
                    finish.run("");
                } catch (IOException e) {
                    logger.error(I18n.txt("error.file.copy", srcString, to.path()));
                }

            } else {
                logger.error(I18n.txt("error.loading.notexistent", srcString));
                if (fail != null)
                    fail.run();
            }
            return null;
        } else {
            // Make a GET request to get data descriptor
            HttpRequest request = new HttpRequest(HttpMethods.GET);
            request.setFollowRedirects(true);
            request.setTimeOut(2500);
            request.setUrl(url);

            // Send the request, listen for the response
            Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
                private boolean cancelled = false;

                @Override
                public void handleHttpResponse(HttpResponse httpResponse) {
                    // Determine how much we have to download
                    long length = Long.parseLong(httpResponse.getHeader("Content-Length"));

                    int status = httpResponse.getStatus().getStatusCode();
                    if (status < 400) {
                        // We're going to download the file, create the streams
                        InputStream is = httpResponse.getResultAsStream();
                        OutputStream os = to.write(false);

                        byte[] bytes = new byte[1024];
                        int count = -1;
                        long read = 0;
                        long lastTimeMs = System.currentTimeMillis();
                        long lastRead = 0;
                        double bytesPerMs = 0;
                        try {
                            logger.info(I18n.txt("gui.download.starting", url));
                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                            DigestInputStream dis = new DigestInputStream(is, md);
                            // Keep reading bytes and storing them until there are no more.
                            while ((count = dis.read(bytes, 0, bytes.length)) != -1 && !cancelled) {
                                os.write(bytes, 0, count);
                                read += count;

                                // Compute progress value
                                final double progressValue = ((double) read / (double) length) * 100;

                                // Compute speed
                                long currentTimeMs = System.currentTimeMillis();
                                // Update each second
                                boolean updateSpeed = currentTimeMs - lastTimeMs >= 1000;
                                if (updateSpeed) {
                                    long elapsedMs = currentTimeMs - lastTimeMs;
                                    long readInterval = read - lastRead;
                                    bytesPerMs = readInterval / elapsedMs;
                                }

                                // Run progress runnable
                                if (progress != null)
                                    progress.run(read, length, progressValue, bytesPerMs);

                                // Reset
                                if (updateSpeed) {
                                    lastTimeMs = currentTimeMs;
                                    lastRead = read;
                                }
                            }
                            is.close();
                            os.close();
                            logger.info(I18n.txt("gui.download.finished", to.path()));

                            // Run finish runnable
                            if (finish != null && !cancelled) {
                                byte[] digestBytes = md.digest();
                                StringBuffer digestString = new StringBuffer();
                                for (int i = 0; i < digestBytes.length; i++) {
                                    if ((0xff & digestBytes[i]) < 0x10) {
                                        digestString.append("0" + Integer.toHexString((0xFF & digestBytes[i])));
                                    } else {
                                        digestString.append(Integer.toHexString(0xFF & digestBytes[i]));
                                    }
                                }
                                String digest = digestString.toString();
                                finish.run(digest);
                            }
                        } catch (Exception e) {
                            logger.error(e);
                            if (fail != null)
                                fail.run();
                        }
                    } else {
                        logger.error(I18n.txt("gui.download.error.httpstatus", status));
                        if (fail != null)
                            fail.run();
                    }
                }

                @Override
                public void failed(Throwable t) {
                    logger.error(I18n.txt("gui.download.fail"));
                    if (fail != null)
                        fail.run();
                }

                @Override
                public void cancelled() {
                    logger.error(I18n.txt("gui.download.cancelled", url));
                    cancelled = true;
                    if (cancel != null)
                        cancel.run();
                }
            });
            return request;
        }
    }


}
