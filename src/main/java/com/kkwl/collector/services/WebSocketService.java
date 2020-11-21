package com.kkwl.collector.services;

import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.models.response.LogViewResponse;
import com.kkwl.collector.services.WebSocketService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {
	public void sendMessage(String message) throws Exception {
		LogViewResponse response = new LogViewResponse();
		response.setContent(message);
		this.simpMessagingTemplate.convertAndSend("/topic/getResponse", response);
	}

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	public void sendMessage() throws InterruptedException {
		while (GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU != null) {
			if (GlobalVariables.GLOBAL_WEBSOCKET_MESSAGE_QUEUE.size() > 0) {
				LogViewResponse response = (LogViewResponse) GlobalVariables.GLOBAL_WEBSOCKET_MESSAGE_QUEUE.poll();
				this.simpMessagingTemplate.convertAndSend("/topic/getResponse", response);
			}

			Thread.sleep(10L);
		}
	}
}

/*
 * Location:
 * C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT
 * .jar!\BOOT-INF\classes\com\kkwl\collector\services\WebSocketService.class
 * Java compiler version: 8 (52.0) JD-Core Version: 1.0.7
 */