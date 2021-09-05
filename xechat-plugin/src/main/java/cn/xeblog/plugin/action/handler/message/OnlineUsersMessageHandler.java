package cn.xeblog.plugin.action.handler.message;

import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.commons.entity.Response;

import java.util.Map;

/**
 * @author anlingyi
 * @date 2020/8/19
 */
public class OnlineUsersMessageHandler extends AbstractMessageHandler<Map<String, String>> {

    @Override
    public void handle(Response<Map<String, String>> response) {
        DataCache.userMap = response.getBody();
        ConsoleAction.setConsoleTitle("Debug(" + DataCache.userMap.size() + ")");
    }
}