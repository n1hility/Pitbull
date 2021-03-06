package org.jboss.pitbull.test;

import org.jboss.pitbull.client.WebSocketBuilder;
import org.jboss.pitbull.server.HttpServer;
import org.jboss.pitbull.server.HttpServerBuilder;
import org.jboss.pitbull.server.handlers.WebSocketHandler;
import org.jboss.pitbull.websocket.BinaryFrame;
import org.jboss.pitbull.websocket.TextFrame;
import org.jboss.pitbull.websocket.WebSocket;
import org.jboss.pitbull.websocket.WebSocketVersion;
import org.jboss.resteasy.util.Hex;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WebSocketTest
{
   public static HttpServer http;

   @BeforeClass
   public static void startup() throws Exception
   {
      http = new HttpServerBuilder().connector().add().connector().https().add()
              .workers(1)
              .maxRequestThreads(1).build();
      http.start();
   }

   @AfterClass
   public static void shutdown() throws Exception
   {
      http.stop();
   }

   public static class TextHandler implements WebSocketHandler
   {
      @Override
      public String getProtocolName()
      {
         return null;
      }

      @Override
      public void onReceivedFrame(WebSocket socket) throws IOException
      {
         TextFrame frame = (TextFrame)socket.readFrame();
         System.out.println("Received: " + frame.getText());
         try
         {
            Thread.sleep(10); // sleep so reads can buffer up.
         }
         catch (InterruptedException e)
         {
            throw new RuntimeException(e);
         }
         socket.writeTextFrame(frame.getText());
      }
   }

   public static class BinaryHandler implements WebSocketHandler
   {
      @Override
      public String getProtocolName()
      {
         return null;
      }

      @Override
      public void onReceivedFrame(WebSocket socket) throws IOException
      {
         BinaryFrame frame = (BinaryFrame)socket.readFrame();
         System.out.println("Server bytes: " + Hex.encodeHex(frame.getBytes()));
         try
         {
            Thread.sleep(10); // sleep so reads can buffer up.
         }
         catch (InterruptedException e)
         {
            throw new RuntimeException(e);
         }
         socket.writeBinaryFrame(frame.getBytes());
      }
   }

   @Test
   public void testFrames() throws Exception
   {
      testTextFrames(WebSocketVersion.HYBI_00);
      testTextFrames(WebSocketVersion.HYBI_13);
      testBinaryFrames(WebSocketVersion.HYBI_13);
   }

   private void testTextFrames(WebSocketVersion version) throws Exception
   {
      TextHandler handler = new TextHandler();
      http.register("/websocket", handler);
      try
      {
         WebSocket socket = WebSocketBuilder.create(version).connect("ws://localhost:8080/websocket");
         testTextFrame(socket);
         socket = WebSocketBuilder.create(version).connect("wss://localhost:8443/websocket");
         testTextFrame(socket);
      }
      finally
      {
         http.unregister(handler);
      }
   }

   private void testTextFrame(WebSocket socket) throws IOException
   {
      socket.writeTextFrame("hello world");
      TextFrame frame = (TextFrame)socket.readFrame();
      Assert.assertEquals("hello world", frame.getText());
      for (int i = 0; i < 10; i++)
      {
         socket.writeTextFrame(Integer.toString(i));
      }
      for (int i = 0; i < 10; i++)
      {
         frame = (TextFrame)socket.readFrame();
         Assert.assertEquals(Integer.toString(i), frame.getText());
      }
   }

   private void testBinaryFrames(WebSocketVersion version) throws Exception
   {
      BinaryHandler handler = new BinaryHandler();
      http.register("/websocket", handler);
      try
      {
         WebSocket socket = WebSocketBuilder.create(version).connect("ws://localhost:8080/websocket");
         testBinaryFrame(socket);
         socket = WebSocketBuilder.create(version).connect("wss://localhost:8443/websocket");
         testBinaryFrame(socket);
      }
      finally
      {
         http.unregister(handler);
      }
   }

   private void testBinaryFrame(WebSocket socket) throws IOException
   {
      ArrayList<byte[]> frames = new ArrayList<byte[]>(10);
      Random random = new Random();

      for (int i = 0; i < 10; i++)
      {
         byte[] bytes = new byte[10];
         random.nextBytes(bytes);
         System.out.println("client bytes: " + Hex.encodeHex(bytes));
         frames.add(bytes);
         socket.writeBinaryFrame(bytes);
      }
      for (int i = 0; i < 10; i++)
      {
         BinaryFrame frame = (BinaryFrame)socket.readFrame();
         byte[] bytes = frames.get(i);
         Assert.assertTrue(Arrays.equals(bytes, frame.getBytes()));
      }
   }
}
