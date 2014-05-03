
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import ir.xweb.server.XWebUser;
import ir.xweb.util.Tools;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Load user information from static file. It support XML, JSON and simple text file. If data file
 * change it will reload it automatically.
 */
public class AuthenticationFileData extends Module implements AuthenticationData {

    private final Logger logger = LoggerFactory.getLogger("AuthenticationFileData");

    public final static String PARAM_FILE = "file";

    private final File file;

    private Long lastUpdate;

    private Map<String, FileUser> defaultSource;

    public AuthenticationFileData(final Manager manager, final ModuleInfo info,
                                  final ModuleParam properties) throws ModuleException {

        super(manager, info, properties);

        file = properties.exists(PARAM_FILE).getFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateUUID(String userId) {
        reloadDataSource();

        final FileUser user = defaultSource.get(userId);
        if(user != null) {
            return user.uuid;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XWebUser getUserWithUUID(String uuid) {
        reloadDataSource();

        for(FileUser u:defaultSource.values()) {
            if(u.uuid != null && u.uuid.equals(uuid)) {
                return u;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XWebUser getUserWithId(String userId, String pass) {
        reloadDataSource();

        final FileUser user = defaultSource.get(userId);
        if(user != null) {
            if(user.password.equals(pass)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Reload DataSource file with last one.
     */
    private void reloadDataSource() {
        if(this.file.exists()) {
            if(this.lastUpdate == null && this.file.lastModified() > this.lastUpdate) {
                final String name = this.file.getName().toLowerCase();

                // or we can use regex = [^\s]+(\.(?i)(txt))$
                if(name.endsWith(".xml")) {
                    defaultSource = importXmlSource(this.file);
                    lastUpdate = this.file.lastModified();
                }
                else if(name.endsWith(".json")) {
                    defaultSource = importJsonSource(this.file);
                    lastUpdate = this.file.lastModified();
                }
                else if(name.endsWith(".xml")) {
                    defaultSource = importXmlSource(this.file);
                    lastUpdate = this.file.lastModified();
                } else {
                    throw new IllegalArgumentException("DataSource format not valid: " + this.file);
                }
            }
        } else {
            logger.error("User authentication not found: " + file);
        }
    }

    private Map<String, FileUser> importXmlSource(final File xmlFile) {
        final SAXBuilder builder = new SAXBuilder();

        try {
            final Map<String, FileUser> source = new HashMap<String, FileUser>();

            final Document document = builder.build(xmlFile);
            final Element rootNode = document.getRootElement();
            final List list = rootNode.getChildren("user");

            for (int i = 0; i < list.size(); i++) {
                final Element u = (Element) list.get(i);

                final String id = u.getAttributeValue("id");
                final String password = Tools.md5(u.getAttributeValue("password"));
                final String role = u.getAttributeValue("role");
                final String uuid = u.getAttributeValue("uuid");

                final FileUser user = new FileUser();
                user.id = id;
                user.password = password;
                user.role = role;
                user.uuid = uuid;

                source.put(id, user);
            }

            return source;
        } catch (IOException ex) {
            logger.error("Error to access XML data source", ex);
        } catch (JDOMException ex) {
            logger.error("Error to parse", ex);
        }

        return null;
    }

    private Map<String, FileUser> importJsonSource(final File jsonFile) {
        try {
            final Map<String, FileUser> source = new HashMap<String, FileUser>();

            final String text = Tools.readTextFile(jsonFile);
            final JSONArray array = new JSONArray(text);

            for (int i = 0; i < array.length(); i++) {
                final JSONObject u = array.getJSONObject(i);

                final String id = u.getString("id");
                final String password = Tools.md5(u.getString("password"));
                final String role = u.getString("role");
                final String uuid = u.getString("uuid");

                final FileUser user = new FileUser();
                user.id = id;
                user.password = password;
                user.role = role;
                user.uuid = uuid;

                source.put(id, user);
            }

            return source;
        } catch (IOException ex) {
            logger.error("Error to access json source", ex);
        } catch (JSONException ex) {
            logger.error("Error to parse json source", ex);
        }

        return null;
    }

    private Map<String, FileUser> importTextSource(final File textFile) {
        try {
            final String text = Tools.readTextFile(textFile);
            final BufferedReader reader = new BufferedReader(new StringReader(text));

            String line;
            while((line = reader.readLine()) != null) {
                if(line.length() > 0) {
                    final String[] parts = line.split("\t");
                    if(parts.length == 3) {
                        final String id = parts[0];
                        final String password = Tools.md5(parts[1]);
                        final String role = parts[2];
                        final String uuid = parts.length > 3 ? null : parts[3];

                        final FileUser user = new FileUser();
                        user.id = id;
                        user.password = password;
                        user.role = role;
                        user.uuid = uuid;

                        defaultSource.put(id, user);
                    } else {
                        logger.error("Illegal line text user source");
                    }
                }
            }
        } catch (IOException ex) {
            logger.error("Error to access text source", ex);
        }

        return null;
    }

    private class FileUser implements XWebUser {

        String id;

        String password;

        String uuid;

        String role;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getRole() {
            return role;
        }

        @Override
        public Object getExtra() {
            return null;
        }

    }

}
