package de.blankedv.sx3pc;
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
			Enumeration<InetAddress> addressesOfAnInterface = ifc.getInetAddresses();

			while (addressesOfAnInterface.hasMoreElements()) {
				InetAddress address = addressesOfAnInterface.nextElement();
                // look for IPv4 addresses which are not==127.0.0.1
				if (!address.equals(localhost) && !address.toString().contains(":")) {
					addrList.add(address);
				//	System.out.println("FOUND ADDRESS ON NIC: " + address.getHostAddress());

				}
			}
		}
		return addrList;
	}
}