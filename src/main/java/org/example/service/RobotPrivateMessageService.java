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
import org.example.config.CaffeineCache;
import org.example.vo.DialogInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

@Slf4j
@Service
public class RobotPrivateMessageService {
    private Client robotClient;
    private final AccessTokenService accessTokenService;

    @Autowired
    private CaffeineCache caffeineCache;

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

    public String send(String text,String userId,String msgId) throws Exception {
        BatchSendOTOHeaders batchSendOTOHeaders = new BatchSendOTOHeaders();
        batchSendOTOHeaders.setXAcsDingtalkAccessToken(accessTokenService.getAccessToken());
        BatchSendOTORequest batchSendOTORequest = new BatchSendOTORequest();
        batchSendOTORequest.setMsgKey("sampleText");
        batchSendOTORequest.setRobotCode(robotCode);
        //回复指定或者多个用户
        batchSendOTORequest.setUserIds(java.util.Arrays.asList(userId));

        JSONObject msgParam = new JSONObject();
        //是否为重复消息
        if (dialogService.isDul(msgId)) {
            //msgParam.put("content", "");
            return null;
        } else {
            //连续对话
            ArrayList<DialogInfo> dialogInfos = dialogService.multiTurn(userId, text);
            String replyAI = chatWithAI(userId, text, dialogInfos);
            msgParam.put("content", replyAI);
        }
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
    private String chatWithAI(String userId, String text, ArrayList<DialogInfo> dialogs) {
        if (StrUtil.isBlank(text)) {
            return "请输入要回复的内容";
        }
        //调用模型
        JSONObject params = new JSONObject();
        params.put("query", JSON.toJSONString(dialogs));

        String result = null;
        try {
            long start = System.currentTimeMillis();
            log.info("===========模型调用开始,用户:{},多轮对话消息:{}=================",userId, JSON.toJSONString(dialogs));

            result = HttpUtil.post(url, JSON.toJSONString(params), timeout);
            long end = System.currentTimeMillis();
            //转成毫秒
            log.info("模型调用耗时:{}ms", end - start);

            JSONObject jsonObject = JSON.parseObject(result);
            String dupResponse = jsonObject.getString("response");
            //简单去掉<think>前面的，保留后面的
            String[] parts = dupResponse.split("</think>", 2);
            String simpleResp = parts.length > 1 ? parts[1].trim() : dupResponse;

            //添加到对话缓存中
            dialogService.addAIHistory(userId, simpleResp);
            log.info("模型调用结束,time:{},用户:{},提问:{},模型过滤结果:{}", LocalDateTime.now(), userId, text, simpleResp);
            return simpleResp;

        } catch (Exception e) {
            String errorMessage;

            // 判断异常类型并设置错误信息
            if (e instanceof com.alibaba.fastjson.JSONException) {
                log.error("模型结果解析异常, path: {}, que: {}, error: {}", url, text, e.getMessage());
                errorMessage = "模型结果解析异常";
            } else {
                log.error("模型调用失败, path: {}, que: {}, error: {}", url, text, e.getMessage());
                errorMessage = "模型调用失败";
            }

            // 删除最后一条对话记录
            dialogService.deleteLast(userId);

            // 返回错误信息
            return errorMessage;
        }
    }

}
