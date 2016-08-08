package com.leejianhao.netty.chapter1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

public class PlainNio2EchoServer {
	public void serve(int port) throws IOException {
		System.out.println("Listening for connections on port " + port);
		final AsynchronousServerSocketChannel serverChannel =
		AsynchronousServerSocketChannel.open();
		InetSocketAddress address = new InetSocketAddress(port);
		serverChannel.bind(address); 
		final CountDownLatch latch = new CountDownLatch(1);
		// 接收新请求，如果一客户端连接被接收到，则调用CompletionHandler
		serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {

			@Override
			public void completed(AsynchronousSocketChannel channel,
					Object attachment) {
				serverChannel.accept(null, this); //接收到客户端连接后，再次accept，接收客户端连接
				ByteBuffer buffer =  ByteBuffer.allocate(100);
				// 触发channel上的读操作,当数据从channel读入buffer完成后，将触发EchocompleteHandler的回调
				channel.read(buffer, buffer, new EchoCompletionHandler(channel)); 
				
			}

			@Override
			public void failed(Throwable exc, Object attachment) {
				try {
					serverChannel.close();
				} catch (IOException e) {
					// ingnore on close
				} finally {
					latch.countDown();
				}
			}
		
		});
		
		try {
			latch.await();
		} catch(InterruptedException e) { 
			Thread.currentThread().interrupt(); // 中断当前线程
		}
	}
	
	private final class EchoCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {
			
		private final AsynchronousSocketChannel channel;
		EchoCompletionHandler(AsynchronousSocketChannel channel) {
			this.channel = channel;
		}
		
		@Override
		public void completed(Integer result, ByteBuffer buffer) {
			buffer.flip();
			// 触发在channel上的写操作，一旦一些东西写入，CompletionHandler将被调用
			channel.write(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
					
				@Override
				public void completed(Integer result, ByteBuffer buffer) {
					if(buffer.hasRemaining()) {
						channel.write(buffer, buffer, this);
					} else {
						buffer.compact();
						channel.read(buffer, buffer, EchoCompletionHandler.this);
					}
				}

				@Override
				public void failed(Throwable exc, ByteBuffer buffer) {
					try {
						channel.close();
					} catch (IOException e) {
						// ingnore on close
					}
				}
				
			});
		}

		@Override
		public void failed(Throwable exc, ByteBuffer attachment) {
			try {
				channel.close();
			} catch (IOException e) {
				// ingnore on close
			}
			
		}
	}
	
}	
