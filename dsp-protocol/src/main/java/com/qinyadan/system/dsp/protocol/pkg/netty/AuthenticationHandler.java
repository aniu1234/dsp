package com.qinyadan.system.dsp.protocol.pkg.netty;

import com.qinyadan.system.dsp.core.config.DspConfiguration;
import com.qinyadan.system.dsp.protocol.command.sqlnode.SqlUseHandler;
import com.qinyadan.system.dsp.constant.ErrorCodeAndMessageEnum;
import com.qinyadan.system.dsp.protocol.pkg.MysqlPackage;
import com.qinyadan.system.dsp.protocol.pkg.auth.LoginRequest;
import com.qinyadan.system.dsp.protocol.pkg.auth.MysqlNativePassword;
import com.qinyadan.system.dsp.protocol.pkg.response.ErrPackage;
import com.qinyadan.system.dsp.protocol.utils.PackageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class AuthenticationHandler extends ChannelInboundHandlerAdapter {

    private static final AtomicBoolean MISSING_CONFIG_LOGGED = new AtomicBoolean();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //???
        final Channel channel = ctx.channel();

        boolean hasAlreadyAuthencaition =
                NettyConnectionHandler.INSTANCE.getAlreadyAuthenChannels().containsKey(channel)
                        && channel.isOpen()
                        && channel.isActive();

        if (hasAlreadyAuthencaition) {
            //已经验证过的话，直接调用下一个handler
            super.channelRead(ctx, msg);

        } else {
            //do authentcaion
            final LoginRequest loginRequest = (LoginRequest) ((MysqlPackage) msg)
                    .getAbstractReaderAndWriterPackage();
            boolean res = doAuthencation(loginRequest, channel);

            MysqlPackage mysqlPackage;

            if (res) {
                final ConnectionContext connectionContext = new ConnectionContext(ctx);
                NettyConnectionHandler.INSTANCE.getAlreadyAuthenChannels().put(channel, connectionContext);
                NettyConnectionHandler.INSTANCE.clearAuthChallenge(channel);

                final String dbName = loginRequest.getDatabase();
                if (Objects.nonNull(dbName)) {
                    //直接set, 如果db不存在的话，后面会关闭连接
                    connectionContext.setDb(loginRequest.getDatabase());
                    mysqlPackage = SqlUseHandler.INSTANCE.useDb(
                            connectionContext, loginRequest.getDatabase());
                } else {
                    mysqlPackage = PackageUtils.buildOkMySqlPackage(0, 2, 0);
                }
            } else {
                mysqlPackage = PackageUtils.buildErrPackage(
                        ErrorCodeAndMessageEnum.PASSWORD_OR_USER_IS_WRONG.getCode(),
                        ErrorCodeAndMessageEnum.PASSWORD_OR_USER_IS_WRONG.getMessage()
                );
            }

            //在认证过程中
            //server----->client 0x00
            //client----->server 0x01
            //server----->client 0x02
            //此处是第三条认证消息
            mysqlPackage.setSeqNumber((byte) 0x02);
            final ByteBuf byteBuf = PackageUtils.packageToBuf(mysqlPackage);
            ctx.writeAndFlush(byteBuf);

            if (!res || mysqlPackage.getAbstractReaderAndWriterPackage() instanceof ErrPackage) {
                //认证错误或者Db不存在的话，直接关闭
                ctx.channel().close();
            }
        }
    }

    private boolean doAuthencation(LoginRequest loginRequest, Channel channel) {
        final String userName = loginRequest.getUserName();
        final byte[] passwordHash = loginRequest.getAuthResponse();
        final byte[] challenge = NettyConnectionHandler.INSTANCE.getAuthChallenge(channel);

        return compareUsernameAndPassword(userName, passwordHash, challenge);
    }

    /**
     * See https://jin-yang.github.io/post/mysql-protocol.html
     *
     * @param userName
     * @param password
     * @return
     */
    private boolean compareUsernameAndPassword(String userName, byte[] passwordHash, byte[] challenge) {
        DspConfiguration configuration = DspConfiguration.load();
        String configuredUser = configuration.getAuthUsername();
        String configuredPassword = configuration.getAuthPassword();
        if (configuredUser == null || configuredPassword == null) {
            if (MISSING_CONFIG_LOGGED.compareAndSet(false, true)) {
                log.warn("Authentication is not configured; set DSP_AUTH_USERNAME and DSP_AUTH_PASSWORD");
            }
            return false;
        }
        return Objects.equals(configuredUser, userName)
                && MysqlNativePassword.matches(configuredPassword, challenge, passwordHash);
    }

}
