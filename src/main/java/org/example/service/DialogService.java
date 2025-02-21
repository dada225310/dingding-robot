package org.example.service;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.config.CaffeineCache;
import org.example.vo.DialogInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
public class DialogService {

    @Autowired
    private CaffeineCache caffeineCache;

    /**
     * 多轮对话
     */
    public ArrayList<DialogInfo> multiTurn(String userId, String text) {

        DialogInfo dialogUser = DialogInfo.toUserInfo(text);

        ArrayList<DialogInfo> list = caffeineCache.getCache().getIfPresent(userId);
        //新会话则创建用户信息
        if ((CollectionUtil.isEmpty(list))) {
            list = CollectionUtil.newArrayList(dialogUser);

            caffeineCache.getCache().put(userId, list);
            return list;
        }
        //添加到多轮对话中
        list.add(dialogUser);
        caffeineCache.getCache().put(userId, list);
        return list;

    }

    /**
     * 机器人回复添加到缓存中
     */
    public void addAIHistory(String userId, String simpleResp) {
        DialogInfo dialogAssistant = DialogInfo.toAssistantInfo(simpleResp);
        ArrayList<DialogInfo> list = caffeineCache.getCache().getIfPresent(userId);
        list.add(dialogAssistant);
        caffeineCache.getCache().put(userId, list);
    }

    /**
     * 删除最后一条对话
     */
    public void deleteLast(String userId) {
        ArrayList<DialogInfo> list = caffeineCache.getCache().getIfPresent(userId);
        list.remove(list.size() - 1);
        caffeineCache.getCache().put(userId, list);
    }

    /**
     * 识别推送的重复消息
     */
    public Boolean isDul(String msgId) {
        String ifPresent = caffeineCache.getDulCache().getIfPresent(msgId);
        if (ifPresent != null) {
            return true;
        }
        caffeineCache.getDulCache().put(msgId, "1");
        return false;
    }

}
