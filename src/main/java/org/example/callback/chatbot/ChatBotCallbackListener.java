package org.example.callback.chatbot;

import com.alibaba.fastjson.JSONObject;
import com.dingtalk.open.app.api.callback.OpenDingTalkCallbackListener;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import com.dingtalk.open.app.api.models.bot.MessageContent;
import lombok.extern.slf4j.Slf4j;
import org.example.service.RobotGroupMessagesService;
import org.example.service.RobotPrivateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 机器人消息回调
 *
 * @author zeymo
 */
@Slf4j
@Component
public class ChatBotCallbackListener implements OpenDingTalkCallbackListener<ChatbotMessage, JSONObject> {
    private RobotGroupMessagesService robotGroupMessagesService;
    @Autowired
    private RobotPrivateMessageService robotPrivateMessageService;

    @Autowired
    public ChatBotCallbackListener(RobotGroupMessagesService robotGroupMessagesService) {
        this.robotGroupMessagesService = robotGroupMessagesService;
    }

    /**
     * https://open.dingtalk.com/document/orgapp/the-application-robot-in-the-enterprise-sends-group-chat-messages
     *
     * @param message
     * @return
     */
    @Override
    public JSONObject execute(ChatbotMessage message) {
        try {
            MessageContent text = message.getText();
            String userId = message.getSenderStaffId();
            if (text != null) {
                String msg = text.getContent();
                String msgId = message.getMsgId();
                log.info("receive bot message from user={}, msg={}", message.getSenderStaffId(), msg);
                String openConversationId = message.getConversationId();
                try {
                    //发送机器人消息 1单聊 2群聊
                    if (message.getConversationType().equals("1")) {
                        robotPrivateMessageService.send(msg, userId, msgId);
                    }else {
                        robotGroupMessagesService.send(openConversationId, msg);
                    }
                } catch (Exception e) {
                    log.error("send group message by robot error:" + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("receive group message by robot error:" + e.getMessage(), e);
        }
        return new JSONObject();
    }
}
