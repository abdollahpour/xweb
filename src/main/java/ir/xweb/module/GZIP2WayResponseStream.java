package ir.xweb.module;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

class GZIP2WayResponseStream extends ServletOutputStream {

    private ServletOutputStream outStream = null;

    private GZIPOutputStream out = null;

    public GZIP2WayResponseStream(HttpServletResponse response) throws IOException {
        outStream = response.getOutputStream();
        out = new GZIPOutputStream(outStream);
        response.addHeader("Content-Encoding", "gzip");
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void flush()  throws IOException {
        out.flush();
    }

    public void finish() throws IOException {
        out.finish();
    }











    /*public void print(java.lang.String s) throws java.io.IOException {
        System.out.println("mode1");
        super.print(s);
    }

    public void print(boolean b) throws java.io.IOException {
        System.out.println("mode2");
        super.print(b);
    }

    public void print(char c) throws java.io.IOException {
        System.out.println("mode3");
        super.print(c);
    }

    public void print(int i) throws java.io.IOException {
        System.out.println("mode4");
        super.print(i);
    }

    public void print(long l) throws java.io.IOException {
        System.out.println("mode5");
        super.print(l);
    }

    public void print(float f) throws java.io.IOException {
        System.out.println("mode6");
        super.print(f);
    }

    public void print(double d) throws java.io.IOException {
        System.out.println("mode7");
        super.print(d);
    }

    public void println() throws java.io.IOException {
        System.out.println("mode8");
        super.println();
    }

    public void println(java.lang.String s) throws java.io.IOException {
        System.out.println("mode9");
        super.println(s);
    }

    public void println(boolean b) throws java.io.IOException {
        System.out.println("mode10");
        super.println(b);
    }

    public void println(char c) throws java.io.IOException {
        System.out.println("mode11");
        super.println(c);
    }

    public void println(int i) throws java.io.IOException {
        System.out.println("mode12");
        super.println(i);
    }

    public void println(long l) throws java.io.IOException {
        System.out.println("mode13");
        super.println(l);
    }

    public void println(float f) throws java.io.IOException {
        System.out.println("mode14");
        super.println(f);
    }

    public void println(double d) throws java.io.IOException {
        System.out.println("mode15");
        super.println(d);
    }*/
}