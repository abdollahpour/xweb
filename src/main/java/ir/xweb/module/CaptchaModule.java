
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import jj.play.ns.nl.captcha.Captcha;
import jj.play.ns.nl.captcha.backgrounds.FlatColorBackgroundProducer;
import jj.play.ns.nl.captcha.gimpy.FishEyeGimpyRenderer;
import jj.play.ns.nl.captcha.text.producer.TextProducer;
import jj.play.ns.nl.captcha.text.renderer.DefaultWordRenderer;
import org.apache.commons.fileupload.FileItem;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/**
 * Generate captcha code/picture and store in session.
 */
public class CaptchaModule extends Module {

    public static int ERROR_CODE_CAPTCHA = 800;

    public static String SESSION_CAPTCHA_CODE = "xweb_captcha_code";

    public static String SESSION_CAPTCHA_EXPIRE = "xweb_captcha_expire";

    public static String SESSION_CAPTCHA_PATTERN = "^[0-9]{5}$";

    public CaptchaModule(
            final Manager manager,
            final ModuleInfo info,
            final ModuleParam properties) throws ModuleException {

        super(manager, info, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final ModuleParam param,
            final Map<String, FileItem> files) throws IOException
    {
        final Random rand = new Random();
        final int code = 10000 + rand.nextInt(89999);

        // just for 10 min
        final long expire = System.currentTimeMillis() + 10 * 60 * 1000;

        final BufferedImage image = createCaptchaImage(code);

        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        long time = System.currentTimeMillis();
        response.setDateHeader("Last-Modified", time);
        response.setDateHeader("Date", time);
        response.setDateHeader("Expires", time);

        final OutputStream outputStream = response.getOutputStream();
        ImageIO.write(image, "jpeg", outputStream);
        outputStream.close();

        request.getSession().setAttribute(SESSION_CAPTCHA_CODE, code);
        request.getSession().setAttribute(SESSION_CAPTCHA_EXPIRE, expire);
    }

    public File createCaptcha(final int code) {
        final BufferedImage image = createCaptchaImage(code);

        final ResourceModule resource = getManager().getModule(ResourceModule.class);

        final File file;
        try {
            if (resource != null) {
                file = resource.initTempFile();
            } else {
                file = File.createTempFile("xweb_captcha_image_" + code, ".jpg");
            }
        }
        catch (IOException ex) {
            throw new IllegalStateException("Error to create temp file", ex);
        }

        try {
            ImageIO.write(image, "jpeg", file);
            return file;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Error to save jpeg file");
        }
    }

    public BufferedImage createCaptchaImage(final int code) {
        final Random rand = new Random();

        java.util.List<Font> textFonts = Arrays.asList(
                //new Font("Arial", Font.PLAIN, 40),
                new Font("Courier", Font.PLAIN, 40));

        // Create random background
        java.awt.Color backgroundColor = new Color(
                200 + rand.nextInt(55),
                200 + rand.nextInt(55),
                200 + rand.nextInt(55)
        );

        // Create random text color
        java.awt.Color textColor = new Color(
                rand.nextInt(55),
                rand.nextInt(55),
                rand.nextInt(55)
        );

        // Create random fish color1
        java.awt.Color fishColor1 = new Color(
                150 + rand.nextInt(55),
                150 + rand.nextInt(55),
                150 + rand.nextInt(55)
        );

        // Create random fish color1
        java.awt.Color fishColor2 = new Color(
                150 + rand.nextInt(55),
                150 + rand.nextInt(55),
                150 + rand.nextInt(55)
        );

        final char[] chars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

        Captcha captcha = new Captcha.Builder(200, 50)
                .addText(
                        new TextProducer() {
                            @Override
                            public String getText() {
                                return Integer.toString(code);
                            }
                        },
                        new DefaultWordRenderer(textColor, textFonts))
                .addBackground(new FlatColorBackgroundProducer(backgroundColor))
                .gimp(new FishEyeGimpyRenderer(fishColor1, fishColor2))
                        //.addNoise()
                        //.addBorder()
                .build();

        return captcha.getImage();
    }

    /**
     * Check captcha code in session is value or not. If will remove captcha if it was successful or not.
     * @param request Request
     * @param captcha Captcha code
     * @throws IOException If captcha code not be value
     * @throws java.lang.IllegalArgumentException If request or captcha be null.
     */
    public void validateOrThrow(final HttpServletRequest request, final String captcha) throws IOException {
        if(request == null) {
            throw new IllegalArgumentException("null request");
        }
        if(captcha == null) {
            throw new IllegalArgumentException("null captcha");
        }

        final Integer code = (Integer) request.getSession().getAttribute(CaptchaModule.SESSION_CAPTCHA_CODE);
        final Long expire = (Long) request.getSession().getAttribute(CaptchaModule.SESSION_CAPTCHA_EXPIRE);

        // remove
        request.getSession().removeAttribute(CaptchaModule.SESSION_CAPTCHA_CODE);
        request.getSession().removeAttribute(CaptchaModule.SESSION_CAPTCHA_EXPIRE);

        // check for captcha
        if(expire != null) {
            if(System.currentTimeMillis() > expire) {
                throw new ModuleException(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    ERROR_CODE_CAPTCHA,
                    "Captcha code expired!",
                    null);
            }

            if(code != Integer.parseInt(captcha)) {
                throw new ModuleException(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    ERROR_CODE_CAPTCHA,
                    "Illegal captcha code: " + captcha,
                    null);
            }
        } else {
            throw new ModuleException(
                HttpServletResponse.SC_UNAUTHORIZED,
                ERROR_CODE_CAPTCHA,
                "Captcha code does not exist!",
                null);
        }
    }

}
