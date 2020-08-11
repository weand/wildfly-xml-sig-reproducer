package com.github.weand.wildfly.xml.sig.reproducer;

import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class SignHandler implements SOAPHandler<SOAPMessageContext> {

    private final static Logger LOGGER = Logger.getLogger(SignHandler.class.getName());

    private static PrivateKey privateKey;

    @Override
    public boolean handleMessage(final SOAPMessageContext messageContext) {

        final Boolean outboundProperty = (Boolean) messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (!outboundProperty.booleanValue()) {
            return true;
        }

        try {
            sign(messageContext);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "error when signing message.", ex);
        }
        return true;
    }

    protected void sign(final SOAPMessageContext messageContext)
        throws SOAPException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, MarshalException, XMLSignatureException {

        final PrivateKey pk = getPrivateKey();

        if (pk == null) {
            LOGGER.severe("private key is null");
            return;
        }

        final SOAPMessage msg = messageContext.getMessage();
        final SOAPBody body = msg.getSOAPBody();
        body.addAttribute(new QName("ID"), "Body");
        body.setIdAttribute("ID", true);

        final XMLSignatureFactory f = XMLSignatureFactory.getInstance("DOM");
        final DOMSignContext dsc = new DOMSignContext(getPrivateKey(), body);

        final List<Transform> transforms = new ArrayList<>();
        transforms.add(f.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        final Reference ref = f.newReference("#Body", f.newDigestMethod(DigestMethod.SHA256, null), transforms, null, null);

        final SignedInfo si = f.newSignedInfo(f.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE,
            (C14NMethodParameterSpec) null),
            f.newSignatureMethod(
                "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
            Collections.singletonList(ref));

        final XMLSignature sig = f.newXMLSignature(si, null);
        sig.sign(dsc);

    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public boolean handleFault(final SOAPMessageContext messageContext) {
        // noop
        return true;
    }

    @Override
    public void close(final MessageContext context) {
        // noop
    }

    protected PrivateKey getPrivateKey() {
        if (privateKey == null) {
            synchronized (this) {
                if (privateKey == null) {
                    privateKey = getPrivateKeyFromKeyStore();
                }
            }
        }
        return privateKey;
    }

    protected PrivateKey getPrivateKeyFromKeyStore() {
        try {
            final String storepass = "secret";
            final String keypass = "secret";
            final String keyalias = "myalias";
            final String keystore = "/privatestore.jks";

            final KeyStore ks = KeyStore.getInstance("JKS");
            try (final InputStream is = SignHandler.class.getClassLoader().getResourceAsStream(keystore)) {
                ks.load(is, storepass.toCharArray());
                final KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(keyalias,
                    new KeyStore.PasswordProtection(keypass.toCharArray()));
                return keyEntry.getPrivateKey();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Fehler beim Lesen des Private Keys.", ex);
            return null;
        }
    }

}
