package com.campusconnect.sis.handler;

import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Loads the homegrown SOAP-header credentials from the classpath.
 *
 * XXX: one shared secret per institution, in a properties file inside the WAR,
 * rotated by redeploying. There is no expiry, no per-consumer credential, and
 * no revocation. The values shipped in this repository are obviously fake
 * placeholders; the real ones are templated in by ansible/deploy.yml.
 */
public final class AuthProperties {

    private static final Logger LOG = Logger.getLogger(AuthProperties.class);

    private static final String RESOURCE = "/auth.properties";

    private static Properties props;

    private AuthProperties() {
    }

    // XXX: unsynchronised lazy init again. Same shape as ConnectionFactory.
    private static Properties props() {
        if (props != null) {
            return props;
        }
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = AuthProperties.class.getResourceAsStream(RESOURCE);
            if (in != null) {
                p.load(in);
            } else {
                LOG.error("auth.properties not found on the classpath; all calls will be rejected");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }
        props = p;
        return props;
    }

    /**
     * @return the configured shared secret for this username, or null.
     */
    public static String secretFor(String username) {
        if (username == null) {
            return null;
        }
        return props().getProperty("sis.auth.user." + username.trim() + ".secret");
    }

    /** @return the institution this username is bound to, or null. */
    public static String customerFor(String username) {
        if (username == null) {
            return null;
        }
        return props().getProperty("sis.auth.user." + username.trim() + ".customer");
    }
}
