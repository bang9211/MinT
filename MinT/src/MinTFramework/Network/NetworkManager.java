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
package MinTFramework.Network;

import MinTFramework.Network.MessageProtocol.CoAPPacket;
import MinTFramework.*;
import MinTFramework.Network.MessageProtocol.PacketDatagram;
import MinTFramework.Network.Protocol.BLE.BLE;
import MinTFramework.Network.Protocol.UDP.UDP;
import MinTFramework.Network.Resource.SendMessage;
import MinTFramework.Network.sharing.Sharing;
import MinTFramework.Network.sharing.routingprotocol.RoutingProtocol;
import MinTFramework.Network.sharing.node.Platforms;
import MinTFramework.SystemScheduler.SystemScheduler;
import MinTFramework.SystemScheduler.MinTthreadPools;
import MinTFramework.Util.ByteBufferPool;
import MinTFramework.Util.DebugLog;
import MinTFramework.storage.ResourceStorage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Network Manager - Managing hold process of networks -
 *
 * @author soobin Jeon <j.soobin@gmail.com>, chungsan Lee <dj.zlee@gmail.com>,
 * youngtak Han <gksdudxkr@gmail.com>
 */
public class NetworkManager {

    private MinT frame = null;
    private ResourceStorage resourceStorage = null;
    private final ArrayList<NetworkType> networkList;
    private final ConcurrentHashMap<NetworkType, Network> networks;
    private String NodeName = null;

    private RoutingProtocol routing = null;
    private Sharing sharing = null;
    //Network Send Adaptor Pool for Send Data
    private SystemScheduler sysSched;

    //for Network Recv ByteBuffer
    private ByteBufferPool bytepool = null;

    //Message Response List
    private final ConcurrentHashMap<Short, SendMSG> ResponseList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Short, SendMSG> IDList = new ConcurrentHashMap<>();
    private PacketIDManager idmaker;

    //Temporary properties for check
    private Integer tempHandlerCnt = 0;
    private Integer sendHandlerCnt = 0;
    private final DebugLog dl;

    /**
     * Auto Set Network Manager as possible
     *
     * @param frame
     */
    public NetworkManager() {
        this.dl = new DebugLog("NetworkManager", true);
        this.networkList = new ArrayList<>();
        this.networks = new ConcurrentHashMap<>();
        this.frame = MinT.getInstance();
        sysSched = frame.getSystemScheduler();
        resourceStorage = frame.getResStorage();
        setNodeName();
//        dl.printMessage("set ByteBuffer");

        if (routing == null) {
            routing = new RoutingProtocol();
        }

        if (sharing == null) {
            sharing = new Sharing();
        }

        makeBytebuffer();
        idmaker = new PacketIDManager(IDList, ResponseList);
    }

    /**
     * Init Routing Algorithm
     */
    private void initSharingSetup() {
        System.out.println("routing initialization");
        //init routing protocol algorithm
        routing.init(this);
        //init sharing approach
        sharing.init(this);
        //start routing algorithm
        sysSched.executeProcess(MinTthreadPools.ROUTING_PROTOCOL, routing);
    }

    /**
     * add network
     *
     * @param ntype
     */
    public void AddNetwork(NetworkType ntype) {
        networkList.add(ntype);
    }

    /**
     * Start when MinT Start
     */
    public void onStart() {
        TurnOnNetwork();
        initSharingSetup();
    }

    /**
     * Turn on All Networks!
     */
    private void TurnOnNetwork() {
        //Setting on Networks
        for (NetworkType ty : networkList) {
            setOnNetwork(ty);
        }
    }

    /**
     * *
     * Set Up the networks Available Networks : UDP, BLE, COAP(asap)
     *
     * @param ntype type of Network NetworkType
     * @param port Internet port for (UDP,TCP/IP,COAP), null for others
     */
    public void setOnNetwork(NetworkType ntype) {
        if (ntype == NetworkType.UDP && networks.get(ntype) == null) {
            System.out.println("Starting UDP...");
//            Network cnet = networks.get(NetworkType.COAP);
//            if(cnet == null)
            networks.put(ntype, new UDP(frame.getNodeName(), ntype));
//            else
//                networks.put(ntype, cnet);
            System.out.println("Turned on UDP: " + ntype.getPort());
        } else if (ntype == NetworkType.BLE && networks.get(ntype) == null) {
            System.out.println("Starting BLE...");
            networks.put(ntype, new BLE(frame.getNodeName()));
            System.out.println("Turned on BLE");
        }
//        } else if (ntype == NetworkType.COAP && networks.get(ntype) == null) { // for CoAP, need to add
//            System.out.println("Starting CoAP...");
//            Network cnet = networks.get(NetworkType.UDP);
//            if(cnet == null)
//                networks.put(ntype, new UDP(frame.getNodeName(),ntype));
//            else
//                networks.put(ntype, cnet);
//            
//            System.out.println("Turned on CoAP");
//        }

        //Turn On All Network
        Iterator it = networks.values().iterator();
        while (it.hasNext()) {
            Network nn = (Network) it.next();
            nn.setRoutingProtocol(routing);
            nn.TurnOnNetwork();
        }
    }

    /**
     * set Routing Protocol
     *
     * @param ap
     */
    public void setRoutingProtocol(RoutingProtocol ap) {
        this.routing = ap;

        for (Network n : networks.values()) {
            n.setRoutingProtocol(ap);
        }
    }

    /**
     * get Routing Protocol
     *
     * @return
     */
    public RoutingProtocol getRoutingProtocol() {
        return routing;
    }

    public void activeRoutingProtocol(String groupName, Platforms platforms) {
        routing.setRoutingProtocol(groupName, platforms);
    }

    public Sharing getSharing() {
        return sharing;
    }

    /**
     * Network Send Method
     *
     * @param smsg
     */
    public void SEND(SendMSG smsg) {
        sysSched.submitProcess(MinTthreadPools.NET_SEND, smsg);
    }
    
    /**
     * Send Response Messages
     * @param rv_packet
     * @param ret 
     */
    public void SEND_RESPONSE(CoAPPacket rv_packet, SendMessage ret) {
        if (rv_packet.getHeader_Type().isCON()) {
            SEND_PIGGYBACK_ACK(rv_packet, (SendMessage) ret);
        } else {
            SEND_SEPERATED_RESPONSE(rv_packet, (SendMessage) ret);
        }
    }
    
    /**
     * *
     * For piggyback acknowledge
     *
     * @param rv_packet Receved packet
     * @param ret
     */
    public void SEND_PIGGYBACK_ACK(PacketDatagram rv_packet, SendMessage ret) {
        // CoAP Piggyback procedure
        if (rv_packet.getMessageProtocolType() == PacketDatagram.MessageProtocol.COAP) {
            CoAPPacket cp = (CoAPPacket)rv_packet;
//            SEND(new SendMSG(CoAPPacket.HEADER_TYPE.ACK, 0,
//                    CoAPPacket.HEADER_CODE.CONTENT, cp.getSource(), ret, cp.getMSGID()));
            SEND(new SendMSG(cp.getMSGID(), CoAPPacket.HEADER_TYPE.ACK, cp.getHeader_TokenLength(),
                    CoAPPacket.HEADER_CODE.CONTENT, cp.getSource(), ret, cp.getToken()));
        } else {
            //non-CoAP Piggyback procedure
        }
    }

    /**
     * @TODO implemantation
     * For separate ack for when piggyback is not possible
     * @param rv_packet received packet
     */
    public void SEND_ACK(CoAPPacket rv_packet) {

    }
    
    public void SEND_SEPERATED_RESPONSE(PacketDatagram rv_packet, SendMessage ret){
        if (rv_packet.getMessageProtocolType() == PacketDatagram.MessageProtocol.COAP) {
            CoAPPacket cp = (CoAPPacket)rv_packet;
            SEND(new SendMSG(idmaker.makeMessageID(), CoAPPacket.HEADER_TYPE.NON, cp.getHeader_TokenLength(),
                                CoAPPacket.HEADER_CODE.CONTENT, cp.getSource(), ret, cp.getToken()));
        } else {
            
        }
    }
    
    /** @deprecated 
     * 
     * @param requestdata 
     */
    public void SEND_UDP_Multicast(SendMessage requestdata) {
        SEND_Multicast(new SendMSG(CoAPPacket.HEADER_TYPE.NON, 2, CoAPPacket.HEADER_CODE.POST, null, requestdata, true));
    }
    
    private void SEND_Multicast(SendMSG smsg) {
        sysSched.submitProcess(MinTthreadPools.NET_SEND, smsg);
    }

    /**
     * get PacketID
     *
     * @return
     */
    public PacketIDManager getIDMaker() {
        return idmaker;
    }

    /**
     * set Node Name
     *
     * @param name
     */
    public void setNodeName(String name) {
        if (name != null) {
            this.NodeName = name;
        }
    }

    /**
     * Return node name
     *
     * @return
     */
    public String getNodeName() {
        return NodeName;
    }

    /**
     * set automatically Node Name
     */
    private void setNodeName() {
        NodeName = "temporary Node";
    }

    /**
     * get Routing group of this node
     *
     * @return
     */
    public String getCurrentRoutingGroup() {
        return routing.getCurrentRoutingGroup();
    }

    /**
     * get Response Data matched by Response ID
     *
     * @param num
     * @return
     */
    public ResponseHandler getResponseDataMatchbyID(short num) {
        SendMSG smsg = ResponseList.get(num);
        if (smsg == null) {
            return null;
        }
        ResponseHandler resd = smsg.getResponseHandler();
        if (resd != null) {
            ResponseList.remove(num);
        }
        return resd;
    }

    /**
     * Temporary Method
     */
    public void setHandlerCount() {
        synchronized (tempHandlerCnt) {
            tempHandlerCnt++;
        }
    }

    public int getHandlerCount() {
        return tempHandlerCnt;
    }

    public synchronized void setSendHandlerCnt() {
        synchronized (sendHandlerCnt) {
            sendHandlerCnt++;
        }
    }

    public int getSendHandlercnt() {
        return sendHandlerCnt;
    }

    /**
     * get Response Msg List
     *
     * @return
     */
    public ConcurrentHashMap<Short, SendMSG> getResponseList() {
        synchronized (ResponseList) {
            return ResponseList;
        }
    }

    /**
     * get Adapted Networks
     *
     * @return
     */
    public ConcurrentHashMap<NetworkType, Network> getNetworks() {
        return this.networks;
    }

    public Network getNetwork(NetworkType ntype) {
        return networks.get(ntype);
    }

    /**
     * get ByteBufferPool
     *
     * @return
     */
    public ByteBufferPool getByteBufferPool() {
        return bytepool;
    }

    /**
     * Make ByteBuffer for Recv Byte Data
     */
    private void makeBytebuffer() {
        String fileName = "bufferpool.dat";
        File bfile = null;
        try {
            bfile = new File(fileName);
            if (!existFile(fileName)) {
                bfile.createNewFile();
            }
            bfile.deleteOnExit();
        } catch (IOException ex) {
            //if Android
            System.out.println("Android File Set up: " + MinTConfig.ANDROID_FILE_PATH + "/bufferpool.dat");
            try {
                fileName = MinTConfig.ANDROID_FILE_PATH + "/bufferpool.dat";
                bfile = new File(fileName);
                if (!existFile(fileName)) {
                    bfile.createNewFile();
                }
                bfile.deleteOnExit();
            } catch (Exception e) {
                System.out.println("Android Error");
                e.printStackTrace();
            }
        }

        try {
            bytepool = new ByteBufferPool(20 * 1024, 40 * 2048, bfile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private Boolean existFile(String isLivefile) {
        File f1 = new File(isLivefile);

        if (f1.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public void putResponse(short responseKey, SendMSG sendmsg) {
        ResponseList.put(responseKey, sendmsg);
//        System.out.println("size : "+ResponseList.size());
    }

    public int getResponseSize() {
        return ResponseList.size();
    }
}
