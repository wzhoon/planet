package cn.xianyijun.planet.remoting.http.servlet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

public class ServletManager {
    public static final int EXTERNAL_SERVER_PORT = -1234;

    private static final ServletManager instance = new ServletManager();

    private final Map<Integer, ServletContext> contextMap = new ConcurrentHashMap<Integer, ServletContext>();

    public static ServletManager getInstance() {
        return instance;
    }

    public void addServletContext(int port, ServletContext servletContext) {
        contextMap.put(port, servletContext);
    }

    public void removeServletContext(int port) {
        contextMap.remove(port);
    }

    public ServletContext getServletContext(int port) {
        return contextMap.get(port);
    }
}
