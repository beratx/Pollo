package mattoncino.pollo;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class JmDnsManager {
    public static int SERVICE_INFO_PORT = 9856;
    private static final String TAG = "ServiceConnManager";
    private String SERVICE_INFO_TYPE = "_pollo_jmdns._tcp.local.";
    private String SERVICE_INFO_NAME = "pollo_jmdns_service";
    private String SERVICE_INFO_PROPERTY_IP_VERSION = "ipv4";
    private String SERVICE_INFO_PROPERTY_DEVICE = "device";


    private JmDNS jmdns = null;
    private ServiceListener listener = null;
    private ServiceInfo serviceInfo;
    private WifiManager.MulticastLock multiCastLock;
    private ServerThreadProcessor serverThreadProcessor;



    public void initializeService(final Context context) {

        WifiManager wifi = (WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);
        changeMultiCastLock(wifi);
        Log.d(TAG, "Multicast lock is changed");

        try {
            if (jmdns == null) {
                InetAddress addr = getInetAddress(wifi);
                //jmdns = JmDNS.create(addr, InetAddress.getLocalHost().getHostName());
                jmdns = JmDNS.create(addr, InetAddress.getLocalHost().getHostName());
                //jmdns = JmDNS.create(addr);
                Log.d(TAG, "JmDNS instance is created");
                //jmdns.addServiceTypeListener();
                jmdns.addServiceListener(SERVICE_INFO_TYPE, listener = new ServiceListener() {
                    public void serviceResolved(ServiceEvent ev) {
                        Log.d(TAG, "service is resolved: " + ev.getInfo());
                    }

                    public void serviceRemoved(ServiceEvent ev) {
                        Log.d(TAG, "service is removed");
                    }

                    public void serviceAdded(ServiceEvent event) {
                        jmdns.requestServiceInfo(event.getType(), event.getName(), true);
                        Log.d(TAG, "service is added: " + "info: " + event.getInfo() +
                                "type: " + event.getType() + "name: " + event.getName());
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

    public boolean initialized(){
        return jmdns != null;
    }

    public void registerService() {
        if (jmdns != null)
            try {
                jmdns.registerService(serviceInfo);
                Log.d(TAG, "JmDNS Service is registered");
            } catch (IOException e) {
                e.printStackTrace();
            }
        /*else{
            initializeService(context);
        }*/
    }

    public void unregisterService() {
        if(serverThreadProcessor != null)
            serverThreadProcessor.terminate();

        if (jmdns != null) {
            jmdns.unregisterService(serviceInfo);
            Log.d(TAG, "JmDNS Service is UNregistered");
            try {
                jmdns.close();
                Log.d(TAG, "JmDNS Service is closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
            jmdns = null;
        }
        if (multiCastLock != null && multiCastLock.isHeld()) {
            multiCastLock.release();
            multiCastLock = null;
            Log.d(TAG, "Multicast lock is released.");
        }
    }


    public String getHostAddress() {
        //String serverIpAddress = getIPv4FromServiceInfo(jmdns.getServiceInfo());
        InetAddress addr = null;
        try {
            if(jmdns != null) {
                //initializeService(MyApplication.getContext());
                addr = jmdns.getInetAddress();
            }
        } catch (IOException e) {
            //e.printStackTrace();
            Log.d(TAG, e.toString());
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

    private String getIPv4FromServiceInfo(ServiceInfo serviceInfo) {
        return serviceInfo.getPropertyString(SERVICE_INFO_PROPERTY_IP_VERSION);
    }

    public Set<String> sendMessageToAllDevicesInNetwork(final Context context, String type, Poll poll) {
        Set<String> ipAddressesSet=null;
        if (jmdns == null) {
            initializeService(context);
        }
        ipAddressesSet = getOnlineDevices(context);

        for (java.util.Iterator iterator = ipAddressesSet.iterator(); iterator.hasNext(); ) {
            String serverIpAddress = (String) iterator.next();
            ClientThreadProcessor clientProcessor = new ClientThreadProcessor(serverIpAddress, context, type, poll);
            Thread t = new Thread(clientProcessor);
            t.start();
        }

        return ipAddressesSet;
    }

     public Set<String> getOnlineDevices(Context context) {

        if (jmdns == null) {
            initializeService(context);
        }

        Set<String> ipAddressesSet = new HashSet<String>();
        //ServiceInfo serviceInfo = jmdns.getServiceInfo(SERVICE_INFO_TYPE, SERVICE_INFO_NAME, false);
        ServiceInfo[] serviceInfoList = jmdns.list(SERVICE_INFO_TYPE);
        String ownDeviceId = ((MyApplication) context.getApplicationContext()).getDeviceId();
        //Log.d(TAG, "serviceInfoList.size() = " + serviceInfoList.length);

        for (int index = 0; index < serviceInfoList.length; index++) {
            String device = serviceInfoList[index].getPropertyString(SERVICE_INFO_PROPERTY_DEVICE);

            if (!device.equals(ownDeviceId)) {
                String serverIpAddress = getIPv4FromServiceInfo(serviceInfoList[index]);
                ipAddressesSet.add(serverIpAddress);
            }
        }
        return ipAddressesSet;
    }
}
