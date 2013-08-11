package ir.xweb.module;

import java.io.*;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;



class GZIP2WayRequestWrapper extends HttpServletRequestWrapper
{

    private HttpServletRequest origRequest;
    private ServletInputStream inStream;
    private BufferedReader reader;

    public GZIP2WayRequestWrapper(HttpServletRequest req)
            throws IOException
    {
        super(req);
        origRequest = null;
        inStream = null;
        reader = null;
        inStream = new GZIP2WayRequestStream(req);
        reader = new BufferedReader(new InputStreamReader(inStream));
    }

    @Override
    public ServletInputStream getInputStream()
            throws IOException
    {
        return inStream;
    }

    @Override
    public BufferedReader getReader()
            throws IOException
    {
        return reader;
    }
}