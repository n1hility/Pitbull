package org.jboss.pitbull.internal.nio.http;


import org.jboss.pitbull.OrderedHeaders;

import java.nio.ByteBuffer;

/**
 * Class that will handle parsing an HTTP request
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HttpRequestDecoder extends HttpMessageDecoder
{
   protected HttpRequestHeader request = new HttpRequestHeader();

   public HttpRequestHeader getRequest()
   {
      return request;
   }

   @Override
   protected OrderedHeaders getHeaders()
   {
      return request.getHeaders();
   }

   @Override
   protected boolean readInitial(ByteBuffer buffer)
   {
      String line = readLine(buffer);
      if (line == null) return false;
      String[] split = splitInitialLine(line);
      if (split.length < 3)
      {
         currentState = States.SKIP_CONTROL_CHARS;
         return true;
      }
      String method = split[0].trim().toUpperCase();
      request.setMethod(method);
      if (method.length() < 2)
      {
         throw new RuntimeException("Parsing request header failed on readInitial: " + line);
      }
      request.setUri(split[1]);
      request.setHttpVersion(split[2].trim().toUpperCase());
      currentState = States.READ_HEADERS;
      return true;
   }


   protected String[] splitInitialLine(String sb)
   {
      int aStart;
      int aEnd;
      int bStart;
      int bEnd;
      int cStart;
      int cEnd;

      aStart = findNonWhitespace(sb, 0);
      aEnd = findWhitespace(sb, aStart);

      bStart = findNonWhitespace(sb, aEnd);
      bEnd = findWhitespace(sb, bStart);

      cStart = findNonWhitespace(sb, bEnd);
      cEnd = findEndOfString(sb);

      return new String[]{
              sb.substring(aStart, aEnd),
              sb.substring(bStart, bEnd),
              cStart < cEnd ? sb.substring(cStart, cEnd) : ""};
   }


}
