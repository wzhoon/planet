package cn.xianyijun.planet.config.api;

import cn.xianyijun.planet.common.Constants;
import cn.xianyijun.planet.common.URL;
import cn.xianyijun.planet.common.Version;
import cn.xianyijun.planet.common.extension.ExtensionLoader;
import cn.xianyijun.planet.registry.api.RegistryFactory;
import cn.xianyijun.planet.registry.api.RegistryService;
import cn.xianyijun.planet.utils.CollectionUtils;
import cn.xianyijun.planet.utils.ConfigUtils;
import cn.xianyijun.planet.utils.NetUtils;
import cn.xianyijun.planet.utils.ReflectUtils;
import cn.xianyijun.planet.utils.StringUtils;
import cn.xianyijun.planet.utils.UrlUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The type Abstract interface config.
 *
 * @author xianyijun
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class AbstractInterfaceConfig extends AbstractMethodConfig {

    /**
     * The Local.
     */
    protected String local;

    /**
     * The Stub.
     */
    protected String stub;

    /**
     * The Proxy.
     */
    protected String proxy;

    /**
     * The Cluster.
     */
    protected String cluster;

    /**
     * The Filter.
     */
    protected String filter;

    /**
     * The Listener.
     */
    protected String listener;

    /**
     * The Connections.
     */
    protected Integer connections;

    /**
     * The Layer.
     */
    protected String layer;

    /**
     * The Application.
     */
    protected ApplicationConfig application;

    /**
     * The Registries.
     */
    protected List<RegistryConfig> registries;

    /**
     * The On connect.
     */
    protected String onConnect;

    /**
     * The On disconnect.
     */
    protected String onDisconnect;

    private Integer callbacks;

    private String scope;

    /**
     * Check application.
     */
    protected void checkApplication() {
        if (application == null) {
            throw new IllegalStateException(
                    "No such application config!");
        }
        appendProperties(application);
        String wait = ConfigUtils.getProperty(Constants.SHUTDOWN_WAIT_KEY);
        if (wait != null && wait.trim().length() > 0) {
            System.setProperty(Constants.SHUTDOWN_WAIT_KEY, wait.trim());
        }
    }

    /**
     * Check registry.
     */
    protected void checkRegistry() {
        if ((registries == null || registries.size() == 0)) {
            throw new IllegalStateException((getClass().getSimpleName().startsWith("Reference")
                    ? "No such any registry to refer service in consumer "
                    : "No such any registry to export service in provider ")
                    + NetUtils.getLocalHost()
                    + " use rpc version "
                    + Version.getVersion());
        }
        for (RegistryConfig registryConfig : registries) {
            appendProperties(registryConfig);
        }
    }

    /**
     * Check stub.
     *
     * @param interfaceClass the interface class
     */
    protected void checkStub(Class<?> interfaceClass) {
        if (ConfigUtils.isNotEmpty(local)) {
            Class<?> localClass = ConfigUtils.isDefault(local) ? ReflectUtils.forName(interfaceClass.getName() + "Local") : ReflectUtils.forName(local);
            if (!interfaceClass.isAssignableFrom(localClass)) {
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceClass.getName());
            }
            try {
                ReflectUtils.findConstructor(localClass, interfaceClass);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No such constructor \"public " + localClass.getSimpleName() + "(" + interfaceClass.getName() + ")\" in local implementation class " + localClass.getName());
            }
        }
        if (ConfigUtils.isNotEmpty(stub)) {
            Class<?> localClass = ConfigUtils.isDefault(stub) ? ReflectUtils.forName(interfaceClass.getName() + "Stub") : ReflectUtils.forName(stub);
            if (!interfaceClass.isAssignableFrom(localClass)) {
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceClass.getName());
            }
            try {
                ReflectUtils.findConstructor(localClass, interfaceClass);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No such constructor \"public " + localClass.getSimpleName() + "(" + interfaceClass.getName() + ")\" in local implementation class " + localClass.getName());
            }
        }
    }

    /**
     * Sets registry.
     *
     * @param registry the registry
     */
    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = new ArrayList<>(1);
        registries.add(registry);
        this.registries = registries;
    }

    /**
     * Load registries list.
     *
     * @param provider the provider
     * @return the list
     */
    List<URL> loadRegistries(boolean provider){
        checkRegistry();
        List<URL> registryList =new ArrayList<>();
        if (CollectionUtils.isEmpty(registries)){
            return registryList;
        }
        for (RegistryConfig config : registries){
            String address = config.getAddress();
            if (StringUtils.isBlank(address)) {
                address = Constants.ANY_HOST_VALUE;
            }
            if (!RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)){
                Map<String, String> map = new HashMap<>();
                appendParameters(map, application);
                appendParameters(map, config);
                map.put("path", RegistryService.class.getName());
                map.put(Constants.RPC_VERSION_KEY,Version.getVersion());
                map.put(Constants.TIMESTAMP_KEY,String.valueOf(System.currentTimeMillis()));

                if (ConfigUtils.getPid() > 0){
                    map.put(Constants.PID_KEY, String.valueOf(ConfigUtils.getPid()));
                }

                if (!map.containsKey(Constants.PROTOCOL_KEY)){
                    if (ExtensionLoader.getExtensionLoader(RegistryFactory.class).hasExtension(Constants.REMOTE_KEY)){
                        map.put(Constants.PROTOCOL_KEY, Constants.REMOTE_KEY);
                    }else {
                        map.put(Constants.PROTOCOL_KEY, Constants.RPC_KEY);
                    }
                }

                List<URL> urls = UrlUtils.parseURLs(address, map);

                if (!CollectionUtils.isEmpty(urls)){
                    registryList.addAll(urls.stream().map(url -> {
                        url = url.addParameter(Constants.REGISTRY_KEY, url.getProtocol());
                        url = url.setProtocol(Constants.REGISTRY_PROTOCOL);
                        return url;
                    }).filter(url -> (provider&&url.getParameter(Constants.REGISTER_KEY, true)) ||(!provider && url.getParameter(Constants.SUBSCRIBE_KEY, true))).collect(Collectors.toList()));
                }
            }
        }
        return registryList;
    }

}
