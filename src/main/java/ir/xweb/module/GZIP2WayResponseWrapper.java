package ir.xweb.module;

/**
 * Created with IntelliJ IDEA.
 * User: hamed
 * Date: 7/7/13
 * Time: 1:56 PM
 * To change this template use File | Settings | File Templates.
 */
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;


class GZIP2WayResponseWrapper extends HttpServletResponseWrapper
{

    private HttpServletResponse response = null;
    private GZIP2WayResponseStream outStream = null;
    private PrintWriter writer = null;

    public GZIP2WayResponseWrapper(HttpServletResponse response) {
        super(response);
        this.response = response;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        /*if(outStream == null)
        {
            String mimeType = (String)header.get(CONTENT_TYPE);
            if(mimeType == null
                    || mimeType.indexOf("text") > -1
                    || mimeType.indexOf("excel") > -1
                    || mimeType.indexOf("word") > -1
                    || mimeType.indexOf("javascript") > -1)
            {
                outStream = new GZIP2WayResponseStream(response);
            }
            else{
                outStream = response.getOutputStream();
            }
        }
        return outStream;*/
        if(outStream == null) {
            outStream = new GZIP2WayResponseStream(response);
        }
        return outStream;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
            outStream.finish();
            outStream.flush();
        } else if (outStream != null) {
            outStream.finish();
            outStream.flush();
        }

        super.flushBuffer();
    }

    @Override
    public void setContentLength(int len) {}

    @Override
    public PrintWriter getWriter() throws IOException {
        if(writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getResponse().getCharacterEncoding()), true);
        }
        return writer;
    }
}