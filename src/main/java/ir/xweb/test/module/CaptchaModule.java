/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.test.module;

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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: hamed
 * Date: 5/20/13
 * Time: 1:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class CaptchaModule extends Module {

    public static int ERROR_CODE_CAPTCHA = 800;

    public static String SESSION_CAPTCHA_CODE = "xweb_captcha_code";

    public static String SESSION_CAPTCHA_EXPIRE = "xweb_captcha_expire";

    public static String SESSION_CAPTCHA_PATTERN = "^[0-9]{5}$";

    private Font font = null;

    public CaptchaModule(
            final Manager manager,
            final ModuleInfo info,
            final ModuleParam properties) throws ModuleException {

        super(manager, info, properties);

        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] allFonts = e.getAllFonts();

        font = allFonts[0];
    }

    @Override
    public void process(
            ServletContext context,
            HttpServletRequest request,
            HttpServletResponse response,
            ModuleParam param, HashMap<String, FileItem> files) throws IOException {

        Random rand = new Random();
        final int code = 10000 + rand.nextInt(89999);

        // just for 10 min
        long expire = System.currentTimeMillis() + 10 * 60 * 1000;

        /*FontGenerator fontGenerator = new FontGenerator() {
            @Override
            public Font getFont() {
                return font;
            }

            @Override
            public int getMinFontSize() {
                return 10;
            }

            @Override
            public int getMaxFontSize() {
                return 15;
            }
        };

        BackgroundGenerator backgroundGenerator = new BackgroundGenerator() {
            @Override
            public int getImageHeight() {
                return 60;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public int getImageWidth() {
                return 150;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public BufferedImage getBackground() {
                BufferedImage image = new BufferedImage(150, 45, BufferedImage.TYPE_INT_RGB); // 123 wide, 123 tall
                Graphics2D g = image.createGraphics();

                g.setColor(Color.WHITE);
                g.fillRect(0, 0, 150, 60);

                //g.setFont(g.getFont().deriveFont(30f));
                //g.setColor(Color.BLACK);
                //g.drawString(Integer.toString(code), 10, 30);

                g.dispose();

                return image;
            }
        };

        TextPaster textPaster = new RandomTextPaster(5, 5, Color.BLACK);

        WordToImage w2i = new ComposedWordToImage(fontGenerator, backgroundGenerator, textPaster);

        BufferedImage image = w2i.getImage(Integer.toString(code));
        */



        /*BufferedImage image = new BufferedImage(150, 45, BufferedImage.TYPE_INT_RGB); // 123 wide, 123 tall
        Graphics2D g = image.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 150, 60);

        g.setFont(g.getFont().deriveFont(30f));
        g.setColor(Color.BLACK);
        g.drawString(Integer.toString(code), 10, 30);

        g.dispose();*/

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

        char[] chars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

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

        BufferedImage image = captcha.getImage();

        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        long time = System.currentTimeMillis();
        response.setDateHeader("Last-Modified", time);
        response.setDateHeader("Date", time);
        response.setDateHeader("Expires", time);

        OutputStream outputStream = response.getOutputStream();
        ImageIO.write(image, "jpeg", outputStream);
        outputStream.close();

        request.getSession().setAttribute(SESSION_CAPTCHA_CODE, code);
        request.getSession().setAttribute(SESSION_CAPTCHA_EXPIRE, expire);
    }

    public static void validateOrThrow(HttpServletRequest request, String captcha) throws IOException {
        Integer code = (Integer) request.getSession().getAttribute(CaptchaModule.SESSION_CAPTCHA_CODE);
        Long expire = (Long) request.getSession().getAttribute(CaptchaModule.SESSION_CAPTCHA_EXPIRE);

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
