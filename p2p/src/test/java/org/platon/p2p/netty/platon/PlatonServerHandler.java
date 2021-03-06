package org.platon.p2p.netty.platon;

import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.platon.common.utils.SpringContextUtil;
import org.platon.p2p.proto.platon.PlatonMessage;
import org.platon.p2p.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatonServerHandler extends SimpleChannelInboundHandler<PlatonMessage> {

    private static final Logger logger = LoggerFactory.getLogger(PlatonServerHandler.class);

    protected ByteString remoteNodeId;

    public ByteString getRemoteNodeId() {
        return remoteNodeId;
    }

    public void setRemoteNodeId(ByteString remoteNodeId) {
        this.remoteNodeId = remoteNodeId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if(remoteNodeId!=null) {
            SessionManager sessionManager = SpringContextUtil.getBean("sessionManager");
            sessionManager.closeSession(remoteNodeId);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PlatonMessage msg) throws Exception {
        logger.debug("Received PlatonMessage:{}", msg);
    }
}
