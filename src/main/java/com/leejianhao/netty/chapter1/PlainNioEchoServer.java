package com.leejianhao.netty.chapter1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class PlainNioEchoServer {
	public void serve(int port) throws IOException {
		System.out.println("Listening for connections on port " + port);
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		ServerSocket ss = serverChannel.socket();
		InetSocketAddress address = new InetSocketAddress(port);
		ss.bind(address);  //1. 将服务绑定到端口
		serverChannel.configureBlocking(false);
		Selector selector = Selector.open();
		serverChannel.register(selector, SelectionKey.OP_ACCEPT); // 注册通道到selector上，关注事件为新的客户端连接被server接收时
		while(true) {
			try {
				selector.select(); //阻塞，等待连接
			} catch (IOException ex) {
				ex.printStackTrace();
				// handle in  a proper way
				break;
			}
			
			Set readyKeys = selector.selectedKeys(); //　得到所有SelectedKey实例
			Iterator iterator = readyKeys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = (SelectionKey) iterator.next();
				iterator.remove(); // 将selectedKey从集合中移除
				try {
					if (key.isAcceptable()) {
						ServerSocketChannel server = (ServerSocketChannel)key.channel();
						SocketChannel client = server.accept(); // 接收客户端连接
						System.out.println("Accepted connection from " + client);
						client.configureBlocking(false);
						client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, ByteBuffer.allocate(100)); // 将连接设置到selector上并设置ByteBuffer
					}
					
					if(key.isReadable()) { // 检查SelectedKey是否可读
						// client 可读取数据
						SocketChannel client = (SocketChannel) key.channel();
						ByteBuffer output = (ByteBuffer) key.attachment();
						client.read(output); // 数据从client读到buffer
						
					}
					
					if(key.isWritable()) {
						// client 可写入数据
						SocketChannel client = (SocketChannel) key.channel();
						ByteBuffer output = (ByteBuffer) key.attachment();
						output.flip(); // 翻转后，准备写入数据
						client.write(output);
						output.compact();
					}
					
				} catch (IOException ex) {
					key.cancel();
					try {
						key.channel().close();
					} catch (IOException cex) {
						
					}
				}
			}
		}
		
	}
	public static void main(String[] args) throws IOException {
		new PlainNioEchoServer().serve(9000);
	}
}
