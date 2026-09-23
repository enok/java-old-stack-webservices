package com.campusconnect.sis.handler;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.log4j.Logger;

/**
 * ==========================================================================
 * SMELL (auth): a homegrown authentication header, hand-parsed in a
 * javax.xml.ws.handler.soap.SOAPHandler.
 *
 * The header looks like this, and is NOT declared anywhere in the WSDL:
 *
 *   &lt;cc:CampusConnectAuth xmlns:cc="http://campusconnect.example.edu/sis/auth/v1"&gt;
 *     &lt;cc:Username&gt;northlake-sis&lt;/cc:Username&gt;
 *     &lt;cc:SharedSecret&gt;...&lt;/cc:SharedSecret&gt;
 *   &lt;/cc:CampusConnectAuth&gt;
 *
 * Everything about this is wrong and all of it is deliberate:
 *   - the secret travels in the SOAP body as cleartext (TLS is the only
 *     protection, and the internal listener is plain HTTP);
 *   - the comparison below is a plain String.equals, so it is not
 *     constant-time;
 *   - there is no nonce, no timestamp and no signature, so any captured
 *     request can be replayed forever;
 *   - the header is undeclared in the contract, so consumers were onboarded
 *     by emailing them an example request;
 *   - the secret is a build-time property file inside the WAR.
 *
 * This is the pre-WS-Security-done-properly pattern. The replacement is
 * mutual TLS plus an OAuth2 client-credentials token at the gateway, and it is
 * sequenced in docs/MODERNIZATION-BACKLOG.md AFTER the characterization tests,
 * because changing the header changes the wire format for all three
 * institutions at once.
 *
 * NOTE: no real credential appears in this repository. auth.properties ships
 * placeholder values.
 * ==========================================================================
 */
public class SharedSecretAuthHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Logger LOG = Logger.getLogger(SharedSecretAuthHandler.class);

    public static final String AUTH_NS = "http://campusconnect.example.edu/sis/auth/v1";
    public static final String AUTH_HEADER = "CampusConnectAuth";

    /** Stashed for the endpoints so they can cross-check the institution. */
    public static final String CTX_CUSTOMER = "campusconnect.auth.customer";
    public static final String CTX_USERNAME = "campusconnect.auth.username";

    public Set<QName> getHeaders() {
        // XXX: returns an empty set, so CXF does not treat the header as
        // "understood". mustUnderstand on this header would fail.
        return Collections.emptySet();
    }

    public boolean handleMessage(SOAPMessageContext context) {

        Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outbound != null && outbound.booleanValue()) {
            return true;
        }

        String username = null;
        String secret = null;

        try {
            SOAPMessage msg = context.getMessage();
            SOAPEnvelope env = msg.getSOAPPart().getEnvelope();
            SOAPHeader header = env.getHeader();

            if (header == null) {
                throw fault("SIS-4010", "missing SOAP header");
            }

            SOAPElement auth = null;
            // Raw Iterator from SAAJ 1.3. No generics.
            for (Iterator it = header.getChildElements(new QName(AUTH_NS, AUTH_HEADER)); it.hasNext();) {
                Object o = it.next();
                if (o instanceof SOAPElement) {
                    auth = (SOAPElement) o;
                    break;
                }
            }
            if (auth == null) {
                throw fault("SIS-4011", "missing " + AUTH_HEADER + " header");
            }

            for (Iterator it = auth.getChildElements(); it.hasNext();) {
                Object o = it.next();
                if (!(o instanceof SOAPElement)) {
                    continue;
                }
                SOAPElement child = (SOAPElement) o;
                String name = child.getElementName().getLocalName();
                if ("Username".equals(name)) {
                    username = child.getValue();
                } else if ("SharedSecret".equals(name)) {
                    secret = child.getValue();
                }
            }

        } catch (SOAPFaultException sfe) {
            throw sfe;
        } catch (Exception e) {
            // SMELL #4 and SMELL #5b together: the parse failure is printed to
            // stdout and then re-thrown as a fault whose faultstring is the
            // raw exception message.
            e.printStackTrace();
            throw fault("SIS-4012", "could not read auth header: " + e.getMessage());
        }

        if (username == null || secret == null) {
            throw fault("SIS-4013", "auth header is incomplete");
        }

        String expected = AuthProperties.secretFor(username);

        // XXX: String.equals on a secret. Not constant-time.
        if (expected == null || !expected.equals(secret)) {
            // XXX: and the rejected username is logged, at WARN, forever.
            LOG.warn("rejected credentials for username=" + username);
            throw fault("SIS-4030", "invalid credentials");
        }

        context.put(CTX_USERNAME, username);
        context.setScope(CTX_USERNAME, MessageContext.Scope.APPLICATION);
        context.put(CTX_CUSTOMER, AuthProperties.customerFor(username));
        context.setScope(CTX_CUSTOMER, MessageContext.Scope.APPLICATION);

        LOG.info("authenticated username=" + username
                + " customer=" + AuthProperties.customerFor(username));
        return true;
    }

    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    public void close(MessageContext context) {
        // nothing
    }

    private static SOAPFaultException fault(String code, String message) {
        try {
            SOAPFault f = javax.xml.soap.SOAPFactory.newInstance().createFault();
            f.setFaultString(code + ": " + message);
            return new SOAPFaultException(f);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(code + ": " + message, e);
        }
    }
}
