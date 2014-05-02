package ir.xweb.test.util;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.io.IOException;

public class TestDownloader {

    @Test
    public void testDownload1() throws IOException {
        //byte[] data = new Downloader("http://www.google.com").retry(3).timeout(1000).download();
        //assertNotEquals(data, null);
    }

    @Test
    public void testDownload2() throws IOException {
        //byte[] data = new Downloader("http://feeds.narenji.ir/Narenji?format=xml").download();
        //assertNotEquals(data, null);
    }

    @Test
    public void testDownload3() throws IOException {
        //InputStream is = new Downloader("http://feeds.narenji.ir/Narenji?format=xml").openStream();
        //assertNotEquals(is, null);
    }


}
