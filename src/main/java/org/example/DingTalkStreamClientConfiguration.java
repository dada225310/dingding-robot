package org.example;

import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.callback.DingTalkStreamTopics;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import org.example.callback.chatbot.ChatBotCallbackListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zeymo
 */
@Configuration
public class DingTalkStreamClientConfiguration {

    @Value("${app.appKey}")
    private String clientId;
    @Value("${app.appSecret}")
    private String clientSecret;

    /**
     * 配置OpenDingTalkClient客户端并配置初始化方法(start)
     *
     * @param chatBotCallbackListener
     * @return
     * @throws Exception
     */
    @Bean(initMethod = "start")
    public OpenDingTalkClient configureStreamClient(@Autowired ChatBotCallbackListener chatBotCallbackListener) throws Exception {
        // init stream client
        return OpenDingTalkStreamClientBuilder.custom()
                //配置应用的身份信息, 企业内部应用分别为appKey和appSecret, 三方应用为suiteKey和suiteSecret
                .credential(new AuthClientCredential(clientId, clientSecret))
                //注册机器人回调
                .registerCallbackListener(DingTalkStreamTopics.BOT_MESSAGE_TOPIC, chatBotCallbackListener).build();
    }
}
