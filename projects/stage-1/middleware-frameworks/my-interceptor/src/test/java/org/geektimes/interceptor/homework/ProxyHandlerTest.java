package org.geektimes.interceptor.homework;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class ProxyHandlerTest {
    public static void main(String[] args) {
        EchoService echoService = new EchoServiceImpl();
        InvocationHandler handler = new ProxyHandler(echoService);
        EchoService proxyEcho = (EchoService) Proxy.newProxyInstance(
                echoService.getClass().getClassLoader(), echoService.getClass().getInterfaces(), handler);
        proxyEcho.echo("哈哈");
        proxyEcho.echo(new Long(1));
        proxyEcho.echo(new Object());
    }
}
