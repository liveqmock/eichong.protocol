package com.webgate.net.sender;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netCore.queue.RepeatMessage;
import com.webgate.service.CacheService;
import com.webgate.utils.DateUtil;

public class PhoneMessageSender {
	
	private static final Logger logger = LoggerFactory.getLogger(PhoneMessageSender.class);
	
	
	public static ChannelFuture  sendMessage(Channel channel,Object object) {
		if(channel == null)
			return null;
		 
		if (channel == null || !channel.isWritable()) {
			return null;
		}
		
		ChannelFuture future = channel.writeAndFlush(object);
		return future;
	}
	
	public static ChannelFuture sendRepeatMessage(Channel channel,byte[] msg,String repeatMsgKey,int version) {
		
		if(channel == null)
		{
			logger.info("sendRepeatMessage repeatMsgKey {} fail channel == null",repeatMsgKey);
			
			return null;
		}
		
		if (!channel.isWritable()) {
			logger.info("sendRepeatMessage repeatMsgKey {} fail channel:{} is not Writable,",repeatMsgKey,channel);
			return null;
		}
		
		channel.writeAndFlush(msg);
		
		//TODO:时间配置，是否重发需要参数化
		//if(GameConfig.resendEpMsgFlag ==1 )
		if(version>=2)
		{
			//RepeatMessage repeatMsg= 
			//		new RepeatMessage(channel,3,GameConfig.resendEpMsgTime,repeatMsgKey,object);
			RepeatMessage repeatMsg= 
					new RepeatMessage(channel,3,30,repeatMsgKey,msg);
			
			repeatMsg.setLastSendTime( DateUtil.getCurrentSeconds());
			CacheService.putPhoneRepeatMsg(repeatMsg);
		}
		
		return null;
	}
	
	
	
	

	
}
