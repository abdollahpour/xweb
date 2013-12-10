/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Downloader {

    private final String DEFAULT_USER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)";

    private final URL url;

    private int retry = 0;

    private int timout;

    private int maxSize;

    private Proxy proxy;

    private String userAgent;

    private List<String> mimeTypes;

    public Downloader(final String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public Downloader(final URL url) {
        this.url = url;
    }

    public Downloader retry(final int count) {
        this.retry = count;

        return this;
    }

    /**
     * Set connection timeout. Remember that timeout can apply after resolve name server by DNS. So. if this process
     * take a long time not error happens. (But there's a default timeout for name server resolving also in most of
     * JREs implementations)
     * @param millis
     * @return
     */
    public Downloader timeout(final int millis) {
        this.timout = millis;

        return this;
    }

    public Downloader maxSize(final int size) {
        this.maxSize = size;

        return this;
    }

    /**
     * You can get list of available UserAgents at <a href='http://www.useragentstring.com/pages/useragentstring.php'>www.useragentstring.com</a>
     * @param userAgent User agent for downloader
     * @return
     */
    public Downloader userAgent(final String userAgent) {
        this.userAgent = userAgent;

        return this;
    }

    /**
     * Set proxy with URI. For http proxy http://hostname:port or https://hostname:port for socket proxy host:port.
     * @param uri Proxy URI. Null URI will be ignore
     * @return
     */
    public Downloader proxy(final String uri) {
        if(uri != null) {
            if(uri.startsWith("http")) {
                try {
                    final URI u = new URI(uri);

                    this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(u.getHost(), u.getPort()));
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException(ex);
                }
            } else {
                final String[] parts = uri.split(":");
                if(parts.length == 2) {
                    this.proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
                } else {
                    throw new IllegalArgumentException("Illegal URI: " + uri);
                }
            }
        }

        return this;
    }

    public Downloader mimeType(String... mimetypes) {
        if(this.mimeTypes == null) {
            this.mimeTypes = new ArrayList<String>();
        }
        this.mimeTypes.addAll(mimeTypes);

        return this;
    }

    public Downloader httpProxy(final String host, final int port) {
        if(host == null) {
            throw new IllegalArgumentException("null host");
        }
        if(port <= 0) {
            throw new IllegalArgumentException("illegal port: " + port);
        }

        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));

        return this;
    }

    public Downloader socketProxy(final String host, final int port) {
        if(host == null) {
            throw new IllegalArgumentException("null host");
        }
        if(port <= 0) {
            throw new IllegalArgumentException("illegal port: " + port);
        }

        this.proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));

        return this;
    }

    public InputStream openStream() throws IOException {
        int r = this.retry;
        do {
            try {
                final URLConnection conn = (proxy == null ? this.url.openConnection() : this.url.openConnection(proxy));

                if(this.timout > 0) {
                    conn.setConnectTimeout(this.timout);
                    conn.setReadTimeout(this.timout);
                }
                conn.addRequestProperty("User-Agent", userAgent == null ? DEFAULT_USER_AGENT : userAgent);

                if(maxSize > 0) {
                    final String contentLength = conn.getHeaderField("Content-Length");
                    if(contentLength != null) {
                        if(Integer.parseInt(contentLength) > maxSize) {
                            throw new IllegalStateException("Illegal size: " + contentLength + " > " + maxSize);
                        }
                    }
                }

                return conn.getInputStream();
            } catch (SocketTimeoutException ex) {
                if(r > 0) {
                    r--;
                } else {
                    throw ex;
                }
            }
        } while (r > -1);

        throw new SocketTimeoutException();
    }

    public byte[] download() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int r = this.retry;
        InputStream is = null;

        do {
            try {
                final URLConnection conn = (proxy == null ? this.url.openConnection() : this.url.openConnection(proxy));

                if(this.timout > 0) {
                    conn.setConnectTimeout(this.timout);
                    conn.setReadTimeout(this.timout);
                }

                final String contentLength = conn.getRequestProperty("Content-Length");
                final String acceptRanges = conn.getRequestProperty("Accept-Ranges");
                final String mimeType = conn.getRequestProperty("Content-Type");

                if(mimeTypes != null && mimeTypes.size() > 0) {
                    if(mimeTypes.contains(mimeType)) {
                        throw new MimeTypeNotFoundException(mimeType + " is not right for: " + url);
                    }
                }

                if(contentLength != null && maxSize > 0) {
                    if(Integer.parseInt(contentLength) > maxSize) {
                        throw new IllegalStateException("Illegal size: " + contentLength + " > " + maxSize);
                    }
                }

                conn.addRequestProperty("User-Agent", userAgent == null ? DEFAULT_USER_AGENT : userAgent);

                if(baos.size() > 0 && acceptRanges != null && acceptRanges.contains("bytes")){
                    conn.setRequestProperty("Range", "bytes=" + baos.size() + "-");
                } else {
                    baos.reset();
                }

                is =  conn.getInputStream();

                int size;
                byte[] buffer = new byte[10240];

                while((size = is.read(buffer)) > 0) {
                    if(maxSize > 0 && size + baos.size() > maxSize) {
                        throw new IllegalStateException("Size is more that max size");
                    }
                    baos.write(buffer, 0, size);
                }

                return baos.toByteArray();
            } catch (SocketTimeoutException ex) {
                if(r > 0) {
                    r--;
                } else {
                    throw ex;
                }
            } finally {
                if(is != null) {
                    try {
                        is.close();
                    } catch (Exception ex) {}
                }
            }
        } while (r > 0);

        throw new SocketTimeoutException();
    }

    public class MimeTypeNotFoundException extends IOException {

        public MimeTypeNotFoundException(final String message) {
            super(message);
        }

    }

}
