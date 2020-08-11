# wildfly-xml-sig-reproducer

After upgrading to WFY20 we are facing a regression regarding WS-Security that was introduced with Upgrade of com.sun.xml.messaging.saaj:saaj-impl in https://issues.redhat.com/browse/WFLY-12442 with WFLY18. When downgrading com.sun.xml.messaging.saaj:saaj-impl to 1.3.x the regression is fixed also in WFLY18+. We did not locate the root cause in saaj-impl 1.4+.

The Bug was spotted within our signing algorithm used for our SOAP Web Services (which uses javax.xml.crypto.dsig Packages).

The Bug can be reproduced easily via https://github.com/weand/wildfly-xml-sig-reproducer, which contains the most basic reproducer code of our scenario:


Reproducer contains a Web Service implementation which uses XML Signature and more specifically the enveloped-signature transform algorithm (https://www.w3.org/TR/xmldsig-core1/#sec-EnvelopedSignature). This standard transform algorithm basically removes the whole Signature element from the digest calculation. And thats not stable since WFLY18 as the Signature element is not removed anymore! The repo also contains an arquillian test testing the SOAP webservice response using rest-assured.


## Run good scenario: Test on WFLY17 

1) `mvn clean install -Pwfly17`
2) Test passes
3) see proper 'Pre-digested input' as DEBUG output of org.apache.jcp Logger (here I pretty formatted the XML):

```
17:58:04,494 DEBUG [org.apache.jcp.xml.dsig.internal.DigesterOutputStream] (default task-1) Pre-digested input:
17:58:04,494 DEBUG [org.apache.jcp.xml.dsig.internal.DigesterOutputStream] (default task-1) <soap:Body xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" ID="Body">
	<ns1:echoResponse
		xmlns:ns1="http://reproducer.sig.xml.wildfly.weand.github.com/">
		<return>hello test</return>
	</ns1:echoResponse>
</soap:Body>
17:58:04,495 DEBUG [org.apache.jcp.xml.dsig.internal.dom.DOMReference] (default task-1) Reference object uri = #Body
17:58:04,495 DEBUG [org.apache.jcp.xml.dsig.internal.dom.DOMReference] (default task-1) Reference digesting completed
```


## Run failing scenario: Test on WFLY18+ 

1) `mvn clean install`    (which defaults to 20.0.1.Final)
2) Test fails
```
[ERROR] testService(com.github.weand.wildfly.xml.sig.reproducer.test.XmlSignatureIT)  Time elapsed: 0.874 s  <<< FAILURE!
java.lang.AssertionError: 

Expected: Expected text value '0FCBFaURtUN+0kxupRbO3pp93rPY+9d1bf7ffAw77lQ=' but was 'A+XljxuKgY2Va+YDk/Ho66i/+JQLeA9QoTH8kap7Zdk=' - comparing <DigestValue ...>0FCBFaURtUN+0kxupRbO3pp93rPY+9d1bf7ffAw77lQ=</DigestValue> at /Envelope[1]/Body[1]/Signature[1]/SignedInfo[1]/Reference[1]/DigestValue[1]/text()[1] to <DigestValue ...>A+XljxuKgY2Va+YDk/Ho66i/+JQLeA9QoTH8kap7Zdk=</DigestValue> at /Envelope[1]/Body[1]/Signature[1]/SignedInfo[1]/Reference[1]/DigestValue[1]/text()[1]:
<DigestValue xmlns="http://www.w3.org/2000/09/xmldsig#">0FCBFaURtUN+0kxupRbO3pp93rPY+9d1bf7ffAw77lQ=</DigestValue>
     but: result was: 
<DigestValue xmlns="http://www.w3.org/2000/09/xmldsig#">A+XljxuKgY2Va+YDk/Ho66i/+JQLeA9QoTH8kap7Zdk=</DigestValue>
        at com.github.weand.wildfly.xml.sig.reproducer.test.XmlSignatureIT.testService(XmlSignatureIT.java:71)
```

3) see invalid 'Pre-digested input' as DEBUG output of org.apache.jcp Logger:

```
18:03:42,888 DEBUG [org.apache.jcp.xml.dsig.internal.DigesterOutputStream] (default task-1) Pre-digested input:
18:03:42,888 DEBUG [org.apache.jcp.xml.dsig.internal.DigesterOutputStream] (default task-1) <soap:Body xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" ID="Body">
	<ns1:echoResponse
		xmlns:ns1="http://reproducer.sig.xml.wildfly.weand.github.com/">
		<return>hello test</return>
	</ns1:echoResponse>
	<Signature
		xmlns="http://www.w3.org/2000/09/xmldsig#">
		<SignedInfo>
			<CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"></CanonicalizationMethod>
			<SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"></SignatureMethod>
			<Reference URI="#Body">
				<Transforms>
					<Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"></Transform>
				</Transforms>
				<DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"></DigestMethod>
				<DigestValue></DigestValue>
			</Reference>
		</SignedInfo>
		<SignatureValue></SignatureValue>
	</Signature>
</soap:Body>
18:03:42,888 DEBUG [org.apache.jcp.xml.dsig.internal.dom.DOMReference] (default task-1) Reference object uri = #Body
18:03:42,888 DEBUG [org.apache.jcp.xml.dsig.internal.dom.DOMReference] (default task-1) Reference digesting completed
```


Again the digest with enveloped-signature transform algorithm works properly when downgrading saaj-impl to 1.3.x in WFLY18+.


Can anybody create a JIRA for the proper component ?


Thanks 
Andreas
