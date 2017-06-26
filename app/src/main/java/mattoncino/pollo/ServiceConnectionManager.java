package mattoncino.pollo;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class ServiceConnectionManager {
    public static int SERVICE_INFO_PORT = 8856;
    private static final String TAG = "ServiceConnManager";
    private String SERVICE_INFO_TYPE = "_pollo_jmdns._tcp.local.";
    private String SERVICE_INFO_NAME = "pollo_jmdns_service";
    private String SERVICE_INFO_PROPERTY_IP_VERSION = "ipv4";
    private String SERVICE_INFO_PROPERTY_DEVICE = "device";


    private JmDNS jmdns = null;
    private ServiceListener listener = null;
    private ServiceInfo serviceInfo;
    private WifiManager.MulticastLock multiCastLock;
    private boolean registered;
    private ServerThreadProcessor serverThreadProcessor;
    private InetAddress ownAddress;


    public void initializeService(final Context context) {

        WifiManager wifi = (WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);
        changeMultiCastLock(wifi);
        Log.d(TAG, "Multicast lock is changed");

        try {
            if (jmdns == null) {

                InetAddress addr = getInetAddress(wifi);
                jmdns = JmDNS.create(addr);
                Log.d(TAG, "JmDNS instance is created");
                jmdns.addServiceListener(SERVICE_INFO_TYPE, listener = new ServiceListener() {
                    public void serviceResolved(ServiceEvent ev) {
                        Log.d(TAG, "service is resolved");
                    }

                    public void serviceRemoved(ServiceEvent ev) {
                        Log.d(TAG, "service is removed");
                    }

                    public void serviceAdded(ServiceEvent event) {
                        jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
                        Log.d(TAG, "service is added");
                    }
                });
                Log.d(TAG, "JmDNS Service Listener is added");
                Hashtable<String, String> settings = setSettingsHashTable(context);
                //(String type, String name, int port, int weight, int priority, boolean persistent, Map<String,?> props)
                serviceInfo = javax.jmdns.ServiceInfo.create(SERVICE_INFO_TYPE, SERVICE_INFO_NAME, SERVICE_INFO_PORT, 0, 0, true, settings);
                //serviceInfo = javax.jmdns.ServiceInfo.create(SERVICE_INFO_TYPE, SERVICE_INFO_NAME, SERVICE_INFO_PORT, 0, 0, "POLLO SERVICE");
                jmdns.registerService(serviceInfo);
                Log.d(TAG, "JmDNS Service is registered");
                serverThreadProcessor = new ServerThreadProcessor(context);
                serverThreadProcessor.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerService() {
        if (jmdns != null)
            try {
                jmdns.registerService(serviceInfo);
                Log.d(TAG, "JmDNS Service is registered");
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void unregisterService() {
        if (jmdns != null)
            jmdns.unregisterService(serviceInfo);
        Log.d(TAG, "JmDNS Service is UNregistered");
    }


    public String getHostAddress() {
        //String serverIpAddress = getIPv4FromServiceInfo(jmdns.getServiceInfo());
        InetAddress addr = null;
        try {
            addr = jmdns.getInetAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addr.getHostAddress();

    }

    private InetAddress getInetAddress(WifiManager wifiManager) throws IOException {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int addrIntIp = wifiInfo.getIpAddress();

        byte[] byteaddr = new byte[]{
                (byte) (addrIntIp & 0xff),
                (byte) (addrIntIp >> 8 & 0xff),
                (byte) (addrIntIp >> 16 & 0xff),
                (byte) (addrIntIp >> 24 & 0xff)
        };
        InetAddress addr = InetAddress.getByAddress(byteaddr);

        return addr;
    }

    private void changeMultiCastLock(WifiManager wifiManager) {
        if (multiCastLock != null && multiCastLock.isHeld())
            multiCastLock.release();

        if (multiCastLock == null) {
            multiCastLock = wifiManager.createMulticastLock("mylockthereturn");
            multiCastLock.setReferenceCounted(true);
        }

        multiCastLock.acquire();
    }

    private Hashtable<String, String> setSettingsHashTable(Context context) {
        Hashtable<String, String> settings = new Hashtable<String, String>();
        settings.put(SERVICE_INFO_PROPERTY_DEVICE, ((MyApplication) context.getApplicationContext()).getDeviceId());
        settings.put(SERVICE_INFO_PROPERTY_IP_VERSION, IPUtils.getLocalIpAddress(context));
        return settings;
    }

    private String getIPv4FromServiceInfo(javax.jmdns.ServiceInfo serviceInfo) {
        return serviceInfo.getPropertyString(SERVICE_INFO_PROPERTY_IP_VERSION);
    }


    public void sendMessageToAllDevicesInNetwork(final Context context, String type, ArrayList<String> messages) {
        if (jmdns != null) {

            Set<String> ipAddressesSet = getNeighborDevicesIpAddressesSet(context);

            for (java.util.Iterator iterator = ipAddressesSet.iterator(); iterator.hasNext(); ) {
                String serverIpAddress = (String) iterator.next();
                //it's NOT a thread!!!
                ClientThreadProcessor clientProcessor = new ClientThreadProcessor(serverIpAddress, context, type, messages);
                Thread t = new Thread(clientProcessor);
                t.start();
                //clientProcessor.sendSimpleMessageToOtherDevice(message);
            }
        }
    }

    /*public void sendMessageToOtherDevice(final Context context, String message) {

    }*/

    private Set<String> getNeighborDevicesIpAddressesSet(Context context) {

        Set<String> ipAddressesSet = new HashSet<String>();
        javax.jmdns.ServiceInfo[] serviceInfoList = jmdns.list(SERVICE_INFO_TYPE);

        for (int index = 0; index < serviceInfoList.length; index++) {
            javax.jmdns.ServiceInfo currentServiceInfo = serviceInfoList[index];

            String device = currentServiceInfo.getPropertyString(SERVICE_INFO_PROPERTY_DEVICE);
            String ownDeviceId = ((MyApplication) context.getApplicationContext()).getDeviceId();

            if (!device.equals(ownDeviceId)) {
                String serverIpAddress = getIPv4FromServiceInfo(currentServiceInfo);
                ipAddressesSet.add(serverIpAddress);
            }
        }
        return ipAddressesSet;
    }

    public List<String> getOnlineDevicesList(Context context, String deviceId) {

        List<String> onlineDevices = new ArrayList<String>();
        try {
            if (jmdns == null) {
                initializeService(context);
            }

            javax.jmdns.ServiceInfo[] serviceInfoList = jmdns.list(SERVICE_INFO_TYPE);
            if (serviceInfoList != null) {

                for (int index = 0; index < serviceInfoList.length; index++) {
                    String device = serviceInfoList[index].getPropertyString(SERVICE_INFO_PROPERTY_DEVICE);

                    try {
                        if (!device.equals(deviceId)) {
                            String ip = getIPv4FromServiceInfo(serviceInfoList[index]);
                            if (!onlineDevices.contains(ip))
                                onlineDevices.add(ip);
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return onlineDevices;
    }


}
