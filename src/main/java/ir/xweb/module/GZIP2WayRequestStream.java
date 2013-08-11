package ir.xweb.module;

import java.io.IOException;
import java.util.zip.GZIPInputStream;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

class GZIP2WayRequestStream extends ServletInputStream
{

    private HttpServletRequest request;
    private ServletInputStream inStream;
    private GZIPInputStream in;

    public GZIP2WayRequestStream(HttpServletRequest request)
            throws IOException
    {
        this.request = null;
        inStream = null;
        in = null;
        this.request = request;
        inStream = request.getInputStream();
        in = new GZIPInputStream(inStream);
    }

    @Override
    public int read()
            throws IOException
    {
        return in.read();
    }

    @Override
    public int read(byte b[])
            throws IOException
    {
        return in.read(b);
    }

    @Override
    public int read(byte b[], int off, int len)
            throws IOException
    {
        return in.read(b, off, len);
    }

    @Override
    public void close()
            throws IOException
    {
        in.close();
    }
}
