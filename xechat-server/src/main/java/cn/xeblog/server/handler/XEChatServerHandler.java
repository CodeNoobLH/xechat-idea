package cn.xeblog.server.handler;

import cn.xeblog.server.action.ChannelAction;
import cn.xeblog.commons.entity.Request;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author anlingyi
 * @date 2020/5/29
 */
@Slf4j
public class XEChatServerHandler extends SimpleChannelInboundHandler<Request> {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        String channelId = ChannelAction.getId(ctx);
        log.debug("客户端连接成功, id -> {}, ip -> {}", channelId, getClientIP(ctx));
    }

    protected void channelRead0(ChannelHandlerContext ctx, Request msg) throws Exception {
        new RequestHandler(ctx, msg).exec();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String id = ChannelAction.getId(ctx);
        log.debug("客户端离线，id -> {}", id);
        ChannelAction.cleanUser(id);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ChannelAction.cleanUser(ctx);
        ctx.close();
        log.error("error：", cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.ALL_IDLE) {
                ctx.close();
            }
        }
    }

    private static String getClientIP(ChannelHandlerContext ctx) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        return inetSocketAddress.getAddress().getHostAddress();
    }
}