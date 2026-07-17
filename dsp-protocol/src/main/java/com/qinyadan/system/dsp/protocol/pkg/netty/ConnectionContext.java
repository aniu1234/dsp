package com.qinyadan.system.dsp.protocol.pkg.netty;

import com.google.common.collect.Maps;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


public class ConnectionContext {

    /**
     * Connection
     */
    @Getter
    private ChannelHandlerContext channelHandlerContext;

    /**
     * if db is not null, this context may use db;
     */
    @Getter
    @Setter
    private String db;

    /**
     * Store the property
     */
    @Getter
    @Setter
    private Map<String, String> properties = Maps.newHashMap();
    /**
     * Store the current query string
     */
    private ThreadLocal<String> queryString = new ThreadLocal<>();

    public ConnectionContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }


    public void write(ByteBuf byteBuf) {
        channelHandlerContext.writeAndFlush(byteBuf);
    }

    public void write(MysqlPackage result) {
        final ByteBuf byteBuf = PackageUtils.packageToBuf(result);
        write(byteBuf);
    }

    public void setQueryString(String sql) {
        queryString.set(sql);
    }

    public String getQueryString() {
        return queryString.get();
    }

}
