/*
 * Copyright (C) 2015 Software&System Lab. Kangwon National University.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package MinTFramework.Network.Protocol.UDP;

import MinTFramework.MinTConfig;
import MinTFramework.Network.NetworkProfile;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ThreadFactory;

/**
 *
 * @author soobin Jeon <j.soobin@gmail.com>, chungsan Lee <dj.zlee@gmail.com>,
 * youngtak Han <gksdudxkr@gmail.com>
 */
public class UDPSendFactory implements ThreadFactory{
    static int threadNo = 0;
    private int port;
    private boolean isMulticast = false;
    private NetworkProfile profile;
    
    public UDPSendFactory(int PORT, boolean _isMulticast, NetworkProfile _profile){
        port = PORT;
        isMulticast = _isMulticast;
        profile = _profile;
    }
    
    public UDPSendFactory(int PORT){
        this(PORT, false, null);
    }

    @Override
    public synchronized Thread newThread(Runnable r) {
        threadNo++;
        InetSocketAddress sendisa = null;
        DatagramChannel sendchannel = null;
        try {
            int nport = port+threadNo;
            sendisa = new InetSocketAddress(nport);
            sendchannel = DatagramChannel.open();
            sendchannel.socket().bind(sendisa);
            if(isMulticast){
                System.out.println("SET Multicast Option to Port: "+nport);
                InetAddress inetaddress = InetAddress.getByName(profile.getIPAddr());
                NetworkInterface interf= NetworkInterface.getByInetAddress(inetaddress);
                InetAddress mulAddress = InetAddress.getByName(MinTConfig.CoAP_MULTICAST_ADDRESS);
                
                sendchannel.setOption(StandardSocketOptions.IP_MULTICAST_IF, interf);
                sendchannel.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 5);
                sendchannel.join(mulAddress, interf);
            }
                
            System.out.println("Sender Factory("+threadNo+"): create sender-"+(port+threadNo));
            return new UDPSendThread(r, sendchannel, "UDP_Sender-"+threadNo);
        } catch (SocketException ex) {
            System.out.println("use addr: "+(port+threadNo));
            ex.printStackTrace();
            return new Thread(r);
        } catch (IOException ex) {
            ex.printStackTrace();
            return new Thread(r);
        }
    }
}
