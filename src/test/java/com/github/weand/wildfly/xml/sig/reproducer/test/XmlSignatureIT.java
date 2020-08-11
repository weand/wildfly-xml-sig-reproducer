package com.github.weand.wildfly.xml.sig.reproducer.test;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.specification.RequestSpecification;

@RunWith(Arquillian.class)
public class XmlSignatureIT {

    private static final Log LOGGER = LogFactory.getLog(XmlSignatureIT.class);

    @ArquillianResource
    private ContainerController controller;

    @Deployment(name = "test")
    @TargetsContainer("server")
    public static WebArchive createBasicDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
            .addPackages(true, "com.github.weand.wildfly.xml.sig.reproducer")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource(new File("src/test/resources/jboss-deployment-structure.xml"))
            .addAsWebInfResource(new File("src/test/resources/web.xml"))
            .addAsResource(
                new File("src/main/resources/com/github/weand/wildfly/xml/sig/reproducer/handlers.xml"),
                "com/github/weand/wildfly/xml/sig/reproducer/handlers.xml")
            .addAsResource(
                new File("src/main/resources/privatestore.jks"),
                "privatestore.jks");
    }

    @RunAsClient
    @Test
    public void testService(@ArquillianResource InitialContext context) throws InterruptedException, ExecutionException, NamingException {

        final RequestSpecification reqSpec = given()
            .contentType("text/xml; charset=\"utf-8\"")
            .body(
                "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><echo xmlns=\"http://reproducer.sig.xml.wildfly.weand.github.com/\"><name>test</name></echo></Body></Envelope>");

        final String response = reqSpec
            .post("http://localhost:8080/test/XmlSignatureService")
            .then()
            .statusCode(200)
            .extract()
            .asString();

        LOGGER.info(response);

        assertThat(response,
            isIdenticalTo(
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body ID=\"Body\"><ns1:echoResponse xmlns:ns1=\"http://reproducer.sig.xml.wildfly.weand.github.com/\"><return>hello test</return></ns1:echoResponse><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><Reference URI=\"#Body\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><DigestValue>0FCBFaURtUN+0kxupRbO3pp93rPY+9d1bf7ffAw77lQ=</DigestValue></Reference></SignedInfo><SignatureValue>I48SJk06NhVsblw0sFUuBcQtaVhGRRTK9vBcdeUhzWwPYEcEztB1bO5PkIgDp/BuLyphyGNMDJQYBuvhAnZ52l737BYvSuuTY1Not/qtBUDWMGnu7FG5NBedtcHdaB1zoD0eodZuJjT1GgOx/vCM3Ynpsf99h23l3rs4Cda9J+9SIBCO6gr/FyJOCYpGLlCxsXpxF50+9lKO7QtbCYCAUWe47VbuQ87PPz1FLWoPdp3LgZcMC+L/MxiJJH12fKXTeovihpKiTjton0oHwKLxA2hNhLmIuvJbli+7PjbJzoRLUe2fJGctQTxUcwH6pjUxb0qr3JPcVcqgToRsxFsh8w==</SignatureValue></Signature></soap:Body></soap:Envelope>")
                    .ignoreComments().ignoreWhitespace().normalizeWhitespace());

    }

}
