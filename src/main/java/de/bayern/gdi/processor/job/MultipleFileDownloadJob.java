/*
 * DownloadClient Geodateninfrastruktur Bayern
 *
 * (c) 2016 GSt. GDI-BY (gdi.bayern.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.bayern.gdi.processor.job;

import de.bayern.gdi.processor.JobExecutionException;
import de.bayern.gdi.utils.CountingInputStream;
import de.bayern.gdi.utils.FileResponseHandler;
import de.bayern.gdi.utils.HTTP;
import de.bayern.gdi.utils.I18n;
import de.bayern.gdi.utils.Log;
import de.bayern.gdi.utils.RemoteFileState;
import de.bayern.gdi.utils.WrapInputStreamFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Abstract class to do multiple file downloads. */
public abstract class MultipleFileDownloadJob extends AbstractDownloadJob {

    private static final Logger LOG = LoggerFactory.getLogger(MultipleFileDownloadJob.class);

    /** Number to re-tries for a failed download. */
    protected static final int MAX_TRIES = 5;
    /** Pause after failed downloads. */
    protected static final long FAIL_SLEEP = 30L * 1000L;

    /** Stores a file location to down from and to. */
    protected static class DLFile {

        /** Destination location of the file. */
        File file;
        /** The url to download from. */
        URL url;
        /** The number of tries yet. */
        int tries;
        /** HTTP post parameters.*/
        HttpEntity postParams;

        DLFile(File file, URL url) {
            this(file, url, null);
        }

        DLFile(File file, URL url,
                HttpEntity postParams) {
            this.file = file;
            this.url = url;
            this.postParams = postParams;
        }

        /**
         * Should we try to download it again?
         * @return true if we should else if we should not.
         */
        protected boolean tryAgain() {
            return ++this.tries < MAX_TRIES;
        }
    }

    /** Number of bytes of the currently downloading file. */
    protected long currentCount;
    /** Total number of bytes of downloaded files do far. */
    protected long totalCount;

    public MultipleFileDownloadJob(String user, String password) {
        this(user, password, null);
    }

    public MultipleFileDownloadJob(String user, String password, Log logger) {
        super(user, password, logger);
        addListener(this);
    }

    @Override
    public void bytesCounted(long count) {
        String msg = I18n.format(
            "atom.bytes.downloaded", this.totalCount + count);
        broadcastMessage(msg);
        this.currentCount = count;
    }

    /**
     * Downloads a file.
     * @param dlf The file to download.
     * @return true if download succeed false otherwise.
     * @throws JobExecutionException If something went wrong.
     * @throws InterruptedException if the job was interrupted.
     */
    protected RemoteFileState downloadFile(DLFile dlf) throws JobExecutionException, InterruptedException {

        final String msg = I18n.format("download.file", dlf.url, dlf.file);
        log(msg);
        LOG.info(msg);
        this.currentCount = 0;
        boolean usePost = dlf.postParams != null;

        HttpRequestBase httpRequest;
        if (usePost) {
            final HttpPost httppost = new HttpPost(dlf.url.toString());
            httppost.setEntity(dlf.postParams);
            httpRequest = httppost;
            LOG.info("WFS GetFeature POST XML request: {}",
                httpPostToString(dlf.url, dlf.postParams));
        } else {
            httpRequest = getGetRequest(dlf.url);
            LOG.info("WFS GetFeature GET KVP request: {}", httpRequest.toString());
        }

        WrapInputStreamFactory wrapFactory
            = CountingInputStream.createWrapFactory(listener);

        CloseableHttpClient client = getClient(dlf.url);
        try {
            FileResponseHandler frh = new FileResponseHandler(dlf.file, wrapFactory, httpRequest);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            HttpDownloadExecutor downloadTask = new HttpDownloadExecutor(client, httpRequest, frh, this);
            Future<?> submit = executorService.submit(downloadTask);
            while (!submit.isDone()) {
                if (Thread.currentThread().isInterrupted()) {
                    httpRequest.abort();
                    throw new InterruptedException("Download interrupted.");
                }
            }
            if (downloadTask.isFailed()) {
                JobExecutionException jobExecutionException = downloadTask.getJobExecutionException();
                Throwable cause = jobExecutionException.getCause();
                if (cause instanceof ClientProtocolException) {
                    return RemoteFileState.FATAL;
                } else if (cause instanceof IOException) {
                    return RemoteFileState.RETRY;
                } else {
                    throw jobExecutionException;
                }
            }
            return RemoteFileState.SUCCESS;
        } finally {
            HTTP.closeGraceful(client);
            this.totalCount += this.currentCount;
        }
    }

    private String httpPostToString(URL url, HttpEntity postParams) {
        try {
            return url.toString() + " " + EntityUtils.toString(postParams);
        } catch (IOException e) {
            // nothing to do
            LOG.info("POST body cannot be logged: "
                + e.getLocalizedMessage());
        }
        return url.toString();
    }

    private static boolean sleep() {
        try {
            Thread.sleep(FAIL_SLEEP);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    /**
     * Downloads a list of files.
     * @param files The files to download.
     * @throws JobExecutionException If something went wrong.
     * @throws InterruptedException if the job was interrupted.
     */
    protected void downloadFiles(List<DLFile> files)
        throws JobExecutionException, InterruptedException {

        broadcastMessage(I18n.format("file.download.start"));

        int numFiles = files.size();

        List<DLFile> successful = new ArrayList<>();
        List<DLFile> again = new ArrayList<>();
        List<DLFile> failed = new ArrayList<>();

        while (!files.isEmpty()) {
            for (DLFile file: files) {
                RemoteFileState rfs = downloadFile(file);
                switch (rfs) {
                    case SUCCESS:
                        successful.add(file);
                        break;
                    case FATAL:
                        failed.add(file);
                        break;
                    default:
                        (file.tryAgain() ? again : failed).add(file);
                        break;
                }
                broadcastMessage(I18n.format(
                    "atom.downloaded.files",
                    successful.size(),
                    Math.min(
                        numFiles,
                        numFiles - failed.size()
                             - successful.size()
                             + again.size())));
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Download of multiple files interrupted.");
                }
            }

            // Only sleep if there are files to try again.
            if (!again.isEmpty() && !sleep()) {
                break;
            }
            files = again;
            again = new ArrayList<>();
        }

        String downloadedBytesMsg =
            I18n.format("atom.bytes.downloaded.total", this.totalCount);
        log(downloadedBytesMsg);
        LOG.info(downloadedBytesMsg);

        if (!failed.isEmpty()) {
            String failedMsg = I18n.format(
                "atom.downloaded.failed", successful.size(), failed.size());
            JobExecutionException jee = new JobExecutionException(failedMsg);
            log(failedMsg);
            throw jee;
        }
        String infosuccess = I18n.format("atom.downloaded.success", successful.size());
        broadcastMessage(infosuccess);
    }
}
