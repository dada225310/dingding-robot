package org.example.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ai历史对话信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DialogInfo {
    private String user = UserRoleEnum.USER.getRole();
    private String query;

    public static DialogInfo toUserInfo(String prompt) {
        DialogInfo info = new DialogInfo();
        info.setUser(UserRoleEnum.USER.getRole());
        info.setQuery(prompt);
        return info;
    }

    public static DialogInfo toAssistantInfo(String prompt) {
        DialogInfo info = new DialogInfo();
        info.setUser(UserRoleEnum.ASSISTANT.getRole());
        info.setQuery(prompt);
        return info;
    }
}
