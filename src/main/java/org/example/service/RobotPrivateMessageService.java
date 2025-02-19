package org.example.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.dingtalkrobot_1_0.Client;
import com.aliyun.dingtalkrobot_1_0.models.BatchSendOTOHeaders;
import com.aliyun.dingtalkrobot_1_0.models.BatchSendOTORequest;
import com.aliyun.dingtalkrobot_1_0.models.BatchSendOTOResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
public class RobotPrivateMessageService {
    private Client robotClient;
    private final AccessTokenService accessTokenService;

    @Autowired
    private DialogService dialogService;
    @Value("${ai.url}")
    private String url;
    @Value("${ai.timeout}")
    private int timeout;

    @Value("${robot.code}")
    private String robotCode;

    @Autowired
    public RobotPrivateMessageService(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    @PostConstruct
    public void init() throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.protocol = "https";
        config.regionId = "central";
        robotClient = new Client(config);
    }

    public String send(String text,String userId) throws Exception {
        BatchSendOTOHeaders batchSendOTOHeaders = new BatchSendOTOHeaders();
        batchSendOTOHeaders.setXAcsDingtalkAccessToken(accessTokenService.getAccessToken());
        BatchSendOTORequest batchSendOTORequest = new BatchSendOTORequest();
        batchSendOTORequest.setMsgKey("sampleText");
        batchSendOTORequest.setRobotCode(robotCode);
        //回复指定或者多个用户
        batchSendOTORequest.setUserIds(java.util.Arrays.asList(userId));

        JSONObject msgParam = new JSONObject();
        //连续对话
        //dialogService.multiTurn(userId, text);
        String replyAI = chatWithAI(text);

        //加入历史对话
        //dialogService.addHistory(userId, text, replyAI);
        msgParam.put("content", replyAI);
        batchSendOTORequest.setMsgParam(msgParam.toJSONString());


        try {
            BatchSendOTOResponse batchSendOTOResponse = robotClient.batchSendOTOWithOptions(batchSendOTORequest, batchSendOTOHeaders, new RuntimeOptions());
            if (Objects.isNull(batchSendOTOResponse) || Objects.isNull(batchSendOTOResponse.getBody())) {
                log.error("RobotPrivateMessages_send batchSendOTOResponse return error, response={}",
                        batchSendOTOResponse);
                return null;
            }
            return batchSendOTOResponse.getBody().getProcessQueryKey();
        } catch (TeaException e) {
            log.error("RobotPrivateMessages_send batchSendOTOResponse throw TeaException, errCode={}, " +
                    "errorMessage={}", e.getCode(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("RobotPrivateMessages_send batchSendOTOResponse throw Exception", e);
            throw e;
        }
    }

    /**
     * 与AI聊天
     */
    private String chatWithAI(String text) {
        if (StrUtil.isBlank(text)) {
            return "请输入要回复的内容";
        }
        //调用模型
        JSONObject params = new JSONObject();
        params.put("prompt", text);
        String jsonParam = JSON.toJSONString(params);

        String result = null;
        try {
            long start = System.currentTimeMillis();
            result = HttpUtil.post(url, jsonParam, timeout);
            long end = System.currentTimeMillis();
            //转成毫秒
            log.info("模型调用耗时:{}ms", end - start);

            JSONObject jsonObject = JSON.parseObject(result);
            String dupResponse = jsonObject.getString("response");
            //简单去掉<think>前面的，保留后面的
            String[] parts = dupResponse.split("</think>", 2);
            String simpleResp = parts.length > 1 ? parts[1].trim() : dupResponse;

            //
            log.info("time:{},用户提问:{},模型结果:{}", LocalDateTime.now(), text, simpleResp);
            return simpleResp;

        } catch (Exception e) {
            if (e instanceof com.alibaba.fastjson.JSONException) {
                log.error("模型结果解析异常,path:{},que:{},error:{}", url, text, e.getMessage());
                return "模型结果解析异常";
            } else {
                log.error("模型调用失败,path:{},que:{},error:{}", url, text, e.getMessage());
                return "模型调用失败";
            }
        }
    }

}
