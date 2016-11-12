package de.blankedv.sx3pc;
import static de.blankedv.sx3pc.InterfaceUI.DEBUG;
import java.net.*;
import java.util.*;

class NIC {

	public static List<InetAddress> getmyip() {

		List<InetAddress> addrList = new ArrayList<InetAddress>();
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			e.printStackTrace();
                        return null;
		}

		InetAddress localhost = null;

		try {
			localhost = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e) {
			e.printStackTrace();
                        return null;
		}

		while (interfaces.hasMoreElements()) {
			NetworkInterface ifc = interfaces.nextElement();
                        //if (DEBUG) System.out.println("Network Interface="+ifc.getName());
			Enumeration<InetAddress> addressesOfAnInterface = ifc.getInetAddresses();

			while (addressesOfAnInterface.hasMoreElements()) {
				InetAddress address = addressesOfAnInterface.nextElement();
                                //if (DEBUG) System.out.println("has address=" + address.getHostAddress());
                // look for IPv4 addresses which are not==127.0.0.1
				if (!address.equals(localhost) && !address.toString().contains(":") && (!ifc.getName().toString().contains("vir"))) {
					addrList.add(address);
                                        if (DEBUG) System.out.println("not local, not ipv6, not virtual =" + address.getHostAddress());
				//	System.out.println("FOUND ADDRESS ON NIC: " + address.getHostAddress());

				}
			}
		}
		return addrList;
	}
}