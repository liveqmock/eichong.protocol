/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.epcentre.server;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.test.ImitateConsumeService;

public class MonitorHttpServerHandler
{
//public class MonitorHttpServerHandler extends SimpleChannelInboundHandler<Object> {
	
	private static final Logger logger = LoggerFactory.getLogger(MonitorHttpServerHandler.class);
	

    private HttpRequest request;
    /** Buffer that stores the response content */
   
    public static String handleGetMessage( String method,Map<String, List<String>> params) throws  IOException
	{
    	StringBuilder buf = new StringBuilder();

    	switch(method)
        {
        case "/getversion":
        {
        	String retDesc=ImitateConsumeService.get_version(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        	
        }
        break;
        case "/connetmonitor":
        {
        	String retDesc=ImitateConsumeService.connetMonitor(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        	
        }
        break;
        case "/getmonitorstat":
        {
        	String retDesc=ImitateConsumeService.getMonitorStat(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        	
        }
        break;
        case "/getBomList":
        {
        	String retDesc=ImitateConsumeService.getBomList(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        	
        }
        break;
        case "/queryversion":
        {
        	String retDesc=ImitateConsumeService.queryVersion(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        	
        }
        break;
        case "/setStopCarOrganValid":
        {
        	String retDesc=ImitateConsumeService.setStopCarOrganValid(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        	
        }
        break;
        case "/force_update_ep_hex":
        {
        	String retDesc=ImitateConsumeService.force_update_ep_hex(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        	
        }
        break;
       
        case "/testDropCarPlace":
        {
        	String retDesc=ImitateConsumeService.testDropCarPlace(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        }
        	break;
        case "/testCallEp":
        {
        	String retDesc=ImitateConsumeService.testCallEp(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        }
        	break;
        case "/testRateCmd":
        {  
        	String retDesc=ImitateConsumeService.testRateCmd(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        }
        break;
        case "/getRate":
        {  
        	String retDesc=ImitateConsumeService.getRatebyId(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        }
        break;
        case "/testStartBespoke":
        {
        	String retDesc=ImitateConsumeService.testStartBespoke(params);
        	if(retDesc!=null)
        		buf.append(retDesc);

        }
        break;  
        case "/testStartBespoke2":
        {
        	String retDesc=ImitateConsumeService.testStartBespoke2(params);
        	if(retDesc!=null)
        		buf.append(retDesc);

        }
        break;
        
        case "/testStopBespoke":
        {
        	
        	String retDesc=ImitateConsumeService.testStopBespoke(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        }
        break;
        case "/testStopBespoke2":
        {
        	
        	String retDesc=ImitateConsumeService.testStopBespoke2(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        }
        break;
        case "/testStartCharge":
        {
        	String retDesc=ImitateConsumeService.testStartCharge(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        }
        break; 	
        case "/testStartCharge2":
        {
        	String retDesc=ImitateConsumeService.testStartCharge2(params);
        	if(retDesc!=null)
        		buf.append(retDesc);
        }
        break;
        case "/testStopCharge":
        {
        	
        	String stopDesc=ImitateConsumeService.testStopCharge(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        	
        }
        break;
        case "/testStopCharge2":
        {
        	
        	String stopDesc=ImitateConsumeService.testStopCharge2(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        	
        }
        break;
        case "/user":
        {
        	
        	String stopDesc=ImitateConsumeService.findUser(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        	
        }
        break;
        case "/stat":
        {
        	String stopDesc=ImitateConsumeService.stat(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
       
        }
        break;
        case "/queryCommSignal":
        {
        	String stopDesc=ImitateConsumeService.queryCommSignal(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
       
        }
        break;
        case "/queryConsumeRecord":
        {
        	String stopDesc=ImitateConsumeService.queryConsumeRecord(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
       
        }
        break;
        case "/testCardAuth":
        {
        	String authDesc=ImitateConsumeService.testCardAuth(params);
        	if(authDesc!=null)
        		buf.append(authDesc);
        }
        
        break;
        case "/createIdentyCode":
        {
        	String stopDesc=ImitateConsumeService.createIdentyCode(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        break;
        case "/gun_restore":
        {
        	String stopDesc=ImitateConsumeService.gun_restore(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        break;
        case "/getepdetail":
        {
        	String stopDesc=ImitateConsumeService.getEpDetail(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        	
        break;
        case "/getstationdetail":
        {
        	String stopDesc=ImitateConsumeService.getStationDetail(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        	
        break;
        case "/getReal":
        {
        	String stopDesc=ImitateConsumeService.getRealData(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        	
        break;
        case "/getLastConsumeRecord":
        {
        	String stopDesc=ImitateConsumeService.getLastConsumeRecord(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        	
        break;
        case "/queryConcentratorConfig":
        {
        	String stopDesc=ImitateConsumeService.getConcentratorConfig(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        	
        break;
        case "/queryRate":
        {
        	String stopDesc=ImitateConsumeService.getRateFromEp(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        	
        break;
        case "/getgundetail":
        {
        	String stopDesc=ImitateConsumeService.getgundetail(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        	
        break;
        case "/removeCharge":
        {
        	String stopDesc=ImitateConsumeService.removeCharge(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        break;
        case "/removebesp":
        {
        	String stopDesc=ImitateConsumeService.removeBespoke(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        	break;
        case "/cleanuser":
        {
        	String stopDesc=ImitateConsumeService.cleanUser(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        	break;
        case "/addwritelist":
        {
        	String stopDesc=ImitateConsumeService.addwritelist(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        break;
        case "/removewritelist":
        {
        	String stopDesc=ImitateConsumeService.removewritelist(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        break;
        case "/openwritelist":
        {
        	String stopDesc=ImitateConsumeService.openwritelist(params);
        	if(stopDesc!=null)
        		buf.append(stopDesc);
        }
        break;
        
        default:
        	break;
        
        };
        
        return buf.toString();
        

	
	}
    
    public static String handlePostMessage(String method,HashMap<String,Object> params) throws  IOException
   	{
    	return "";
   	}
    
    /*
    
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest request = this.request = (HttpRequest) msg;
            
            buf.setLength(0);
           
            String uri=request.getUri();
            //buf.append("REQUEST_URI: ").append(uri).append("\r\n\r\n");

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
            String path = queryStringDecoder.path();
            
            appendDecoderResult(buf, request);
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;

            ByteBuf content = httpContent.content();
            if (content.isReadable()) {
                buf.append("CONTENT: ");
                buf.append(content.toString(CharsetUtil.UTF_8));
                buf.append("\r\n");
                appendDecoderResult(buf, request);
            }

            if (msg instanceof LastHttpContent) {
                LastHttpContent trailer = (LastHttpContent) msg;
                if (!trailer.trailingHeaders().isEmpty()) {
                    buf.append("\r\n");
                 
                    buf.append("\r\n");
                }

                writeResponse(trailer, ctx);
                
               // ctx.channel().close();
             }
        }
    }
    
    private static void appendDecoderResult(StringBuilder buf, HttpObject o) {
        DecoderResult result = o.getDecoderResult();
        if (result.isSuccess()) {
            return;
        }

        buf.append(".. WITH DECODER FAILURE: ");
        buf.append(result.cause());
        buf.append("\r\n");
    }

    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        // Decide whether to close the connection or not.
        boolean keepAlive = isKeepAlive(request);
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, currentObj.getDecoderResult().isSuccess()? OK : BAD_REQUEST,
                Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Encode the cookie.
        String cookieString = request.headers().get(COOKIE);
        if (cookieString != null) {
            Set<Cookie> cookies = CookieDecoder.decode(cookieString);
            if (!cookies.isEmpty()) {
                // Reset the cookies if necessary.
                for (Cookie cookie: cookies) {
                    response.headers().add(SET_COOKIE, ServerCookieEncoder.encode(cookie));
                }
            }
        } else {
            // Browser sent no cookie.  Add some.
            response.headers().add(SET_COOKIE, ServerCookieEncoder.encode("key1", "value1"));
            response.headers().add(SET_COOKIE, ServerCookieEncoder.encode("key2", "value2"));
        }

        // Write the response.
        ctx.write(response);

        return keepAlive;
    }
	*/
   
    
}
