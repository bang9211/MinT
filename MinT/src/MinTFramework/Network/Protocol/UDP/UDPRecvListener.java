/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MinTFramework.Network.Protocol.UDP;

import MinTFramework.Network.NetworkManager;
import MinTFramework.Util.ByteBufferPool;
import MinTFramework.Util.DebugLog;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 *
 * @author soobin
 */
public class UDPRecvListener extends Thread{
    Selector selector;
    DatagramChannel channel;
    UDP udp = null;
    NetworkManager networkmanager = null;
    DebugLog dl = new DebugLog("UDPRecvAdaptor");
    
    public UDPRecvListener(DatagramChannel channel, UDP udp) throws IOException{
        this.udp = udp;
        networkmanager = udp.getNetworkManager();
        selector = Selector.open();
        this.channel = channel;
        channel.register(selector, SelectionKey.OP_READ);
    }

    @Override
    public void run() {
        try{
            while(!Thread.currentThread().isInterrupted()){
//                dl.printMessage(this.getID()+"-wait selector..");
                int KeysReady = selector.select(500);
//                dl.printMessage("Selector Accepted");
                RequestPendingConnection();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void RequestPendingConnection(){
//        dl.printMessage("in the RequestPending..");
        Iterator selectedKeys = selector.selectedKeys().iterator();
        while(selectedKeys.hasNext()){
                SelectionKey key = (SelectionKey) selectedKeys.next();
                selectedKeys.remove();
                if(!key.isValid())
                    continue;
                if(key.isReadable())
                    read(key);
        }
    }
    
    private void read(SelectionKey key){
//        dl.printMessage("in the read");
        ByteBufferPool bbp = networkmanager.getByteBufferPool();
        ByteBuffer req = null;
        byte[] fwdbyte = null;
        SocketAddress rd = null;
        try{
            req = bbp.getMemoryBuffer();
            DatagramChannel chan = (DatagramChannel)key.channel();
            //read
            rd = chan.receive(req);
            dl.printMessage("readed limit : "+req.limit()+" byte Readed");
            //sort pointer
            req.flip();
            
            //make received byte
            fwdbyte = new byte[req.limit()];
            req.get(fwdbyte, 0, req.limit());
            dl.printMessage("req bytebuffer : "+req.limit()+" byte Readed");
            dl.printMessage("fwd byte : "+fwdbyte.length+" byte Readed");
            
            //send byte to handler
            udp.putReceiveHandler(fwdbyte);
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            bbp.putBuffer(req);
        }
    }
}