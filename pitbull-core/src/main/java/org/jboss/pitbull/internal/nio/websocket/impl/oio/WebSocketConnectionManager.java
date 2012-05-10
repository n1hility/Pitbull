package org.jboss.pitbull.internal.nio.websocket.impl.oio;

import org.jboss.pitbull.internal.logging.Logger;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.Handshake;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.WebSocketHeaders;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.protocol.ietf00.Hybi00Handshake;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.protocol.ietf07.Hybi07Handshake;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.protocol.ietf08.Hybi08Handshake;
import org.jboss.pitbull.internal.nio.websocket.impl.oio.internal.protocol.ietf13.Hybi13Handshake;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WebSocketConnectionManager
{
   private static final List<Handshake> websocketHandshakes;
   protected static final Logger log = Logger.getLogger(WebSocketConnectionManager.class);

   static
   {
      final List<Handshake> handshakeList = new ArrayList<Handshake>();
      handshakeList.add(new Hybi13Handshake());
      handshakeList.add(new Hybi07Handshake());
      handshakeList.add(new Hybi08Handshake());
      handshakeList.add(new Hybi00Handshake());

      websocketHandshakes = Collections.unmodifiableList(handshakeList);
   }

   public static OioWebSocket establish(String protocolName, HttpRequestBridge request, HttpResponseBridge response, ClosingStrategy closingStrategy) throws IOException
   {
      for (Handshake handshake : websocketHandshakes)
      {
         if (handshake.matches(request))
         {
            return handshake(protocolName, request, response, closingStrategy, handshake);

         }
      }
      if (WebSocketHeaders.SEC_WEBSOCKET_VERSION.isIn(request))
      {
         log.warn("Unsupported web socket protocol: " + WebSocketHeaders.SEC_WEBSOCKET_VERSION.get(request));
      }
      else
      {
         log.warn("Unsupported web socket protocol");
      }
      return null;
   }



   public static OioWebSocket handshake(String protocolName, HttpRequestBridge request, HttpResponseBridge response, ClosingStrategy closingStrategy, Handshake handshake) throws IOException
   {
      /**
       * We found a matching handshake, so let's tell the web server we'd like to begin the process of
       * upgrading this connection to a WebSocket.
       */
      response.startUpgrade();

      //log.debug("Found a compatible handshake: (Version:"
      //        + handshake.getVersion() + "; Handler: " + handshake.getClass().getName() + ")");

      /* Sets the standard upgrade headers that are common to all HTTP 101 upgrades, as well as the
* SEC_WEBSOCKETS_PROTOCOL header (if the protocol is specified) common to all WebSocket implementations.
*/
      response.setHeader("Upgrade", "WebSocket");
      response.setHeader("Connection", "Upgrade");

      if (protocolName != null)
         WebSocketHeaders.SEC_WEBSOCKET_PROTOCOL.set(response, protocolName);
      else
      {
         WebSocketHeaders.SEC_WEBSOCKET_PROTOCOL.set(response, "*");
      }

      /**
       * Generate the server handshake response -- setting the necessary headers and also capturing
       * any data bound for the body of the response.
       */
      final byte[] handShakeData = handshake.generateResponse(request, response);

      // write the handshake data
      response.getOutputStream().write(handShakeData);

      /**
       * Obtain an WebSocket instance from the handshaker.
       */
      final OioWebSocket webSocket
              = handshake.getServerWebSocket(request, response, closingStrategy);

      //log.debug("Using WebSocket implementation: " + webSocket.getClass().getName());

      response.sendUpgrade();
      return webSocket;
   }
}
