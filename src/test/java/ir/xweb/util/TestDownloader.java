package ir.xweb.util;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.io.IOException;

public class TestDownloader {

    @Test
    public void testDownload() throws IOException {
        byte[] data = new Downloader("http://www.google.com").retry(3).timeout(1000).download();
        assertNotEquals(data, null);
    }

}
