package com.leejianhao.netty.chapter2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class EchoServer {
	private final int port;
	public EchoServer(int port) {
		this.port = port;
	}
	
	public void start() throws Exception {
		
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap(); // #1 开启服务
			b.group(group)                              // #2 指定Nio group
			.channel(NioServerSocketChannel.class)      // #2 指定NIO 传输   //OioServerSocketChannel
			.localAddress(new InetSocketAddress(port))  // #2 制定socket 端口
			.childHandler(new ChannelInitializer<SocketChannel>() { // #3增加handler到管道中

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new EchoServerHandler());  //#4 绑定server，等待server关闭和释放资源
				}
			});
			
			ChannelFuture f = b.bind().sync(); //# 5绑定服务，然后等待绑定完成,sync将阻塞直到服务边界
			System.out.println(EchoServer.class.getName() + 
					" started and listen on "+f.channel().localAddress());
			f.channel().closeFuture().sync(); // 同步等待服务通道关闭
			
		} finally {
			group.shutdownGracefully().sync();
		}
	}
}
