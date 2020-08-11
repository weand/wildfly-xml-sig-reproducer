package com.github.weand.wildfly.xml.sig.reproducer;

import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService(targetNamespace = "http://reproducer.sig.xml.wildfly.weand.github.com/", name = "XmlSignatureService")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "handlers.xml")
public class XmlSignatureService {

    @WebResult(name = "return", targetNamespace = "http://reproducer.sig.xml.wildfly.weand.github.com/", partName = "return")
    @WebMethod
    public String echo(@WebParam(partName = "name", name = "name") String name) {
        return "hello " + name;
    }
}
