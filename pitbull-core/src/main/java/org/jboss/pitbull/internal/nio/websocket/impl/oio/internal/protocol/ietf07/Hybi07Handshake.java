/*
 * Copyright 2012 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.protocol.ietf07;


import org.jboss.pitbull.internal.NotImplementedYetException;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.ClosingStrategy;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.HttpRequestBridge;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.HttpResponseBridge;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.OioWebSocket;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.Handshake;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.WebSocketHeaders;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.WebSocketHeaders.*;

/**
 * The handshaking protocol implementation for Hybi-07.
 *
 * @author Mike Brock
 */
public class Hybi07Handshake extends Handshake
{
   protected Hybi07Handshake(final String version)
   {
      super(version, "SHA1", "258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
   }

   public Hybi07Handshake()
   {
      this("7");
   }

   @Override
   public OioWebSocket getServerWebSocket(final HttpRequestBridge request,
                                          final HttpResponseBridge response,
                                          final ClosingStrategy closingStrategy) throws IOException
   {
      return new Hybi07Socket(getVersion(), URI.create(getWebSocketLocation(request)), request.getInputStream(), response.getOutputStream(), closingStrategy);
   }

   @Override
   public OioWebSocket getClientWebSocket(URI uri, InputStream inputStream, OutputStream outputStream, ClosingStrategy closingStrategy) throws IOException
   {
      throw new NotImplementedYetException();
   }

   @Override
   public boolean matches(final HttpRequestBridge request)
   {
      return (SEC_WEBSOCKET_KEY.isIn(request) && SEC_WEBSOCKET_VERSION.matches(request, getVersion()));
   }

   @Override
   public byte[] generateResponse(final HttpRequestBridge request,
                                  final HttpResponseBridge response) throws IOException
   {

      if (ORIGIN.isIn(request))
      {
         SEC_WEBSOCKET_ORIGIN.set(response, ORIGIN.get(request));
      }

      SEC_WEBSOCKET_PROTOCOL.copy(request, response);

      SEC_WEBSOCKET_LOCATION.set(response, getWebSocketLocation(request));

      final String key = SEC_WEBSOCKET_KEY.get(request);
      final String solution = solve(key);

      WebSocketHeaders.SEC_WEBSOCKET_ACCEPT.set(response, solution);

      return new byte[0];
   }

   public String solve(final String nonceBase64)
   {
      try
      {
         final String concat = nonceBase64.trim().concat(getMagicNumber());
         final MessageDigest digest = MessageDigest.getInstance(getHashAlgorithm());
         digest.update(concat.getBytes("UTF-8"));
         final String result = Base64.encodeBase64String(digest.digest()).trim();
//
//      System.out.println("Browser Key: '" + nonceBase64 + "'");
//      System.out.println("Concat     : '" + concat + "'");
//      System.out.println("Result     : '" + result + "'");

         return result;
      }
      catch (NoSuchAlgorithmException e)
      {
         throw new RuntimeException("error generating hash", e);
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException("could not get UTF-8 bytes");
      }
   }
}
