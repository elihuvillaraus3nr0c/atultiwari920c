/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.iot.mqtt.core.server;

import net.dreamlu.iot.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.event.IMqttConnectStatusListener;
import net.dreamlu.iot.mqtt.core.server.model.Message;
import net.dreamlu.iot.mqtt.core.server.session.IMqttSessionManager;
import net.dreamlu.iot.mqtt.core.server.store.IMqttMessageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;
import org.tio.core.DefaultAioListener;
import org.tio.core.Tio;
import org.tio.core.TioConfig;
import org.tio.core.intf.Packet;
import org.tio.http.common.HttpConst;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.utils.hutool.StrUtil;

/**
 * mqtt 服务监听
 *
 * @author L.cm
 */
public class MqttServerAioListener extends DefaultAioListener {
	private static final Logger logger = LoggerFactory.getLogger(MqttServerAioListener.class);
	private final IMqttMessageStore messageStore;
	private final IMqttSessionManager sessionManager;
	private final IMqttMessageDispatcher messageDispatcher;
	private final IMqttConnectStatusListener connectStatusListener;

	public MqttServerAioListener(MqttServerCreator serverCreator) {
		this.messageStore = serverCreator.getMessageStore();
		this.sessionManager = serverCreator.getSessionManager();
		this.messageDispatcher = serverCreator.getMessageDispatcher();
		this.connectStatusListener = serverCreator.getConnectStatusListener();
	}

	@Override
	public boolean onHeartbeatTimeout(ChannelContext context, Long interval, int heartbeatTimeoutCount) {
		String clientId = context.getBsId();
		logger.info("Mqtt HeartbeatTimeout clientId:{} interval:{} count:{}", clientId, interval, heartbeatTimeoutCount);
		return false;
	}

	@Override
	public void onBeforeClose(ChannelContext context, Throwable throwable, String remark, boolean isRemove) {
		// 1. http 请求跳过
		boolean isHttpRequest = context.get(MqttConst.IS_HTTP) != null;
		if (isHttpRequest) {
			return;
		}
		// 2. 业务 id
		String clientId = context.getBsId();
		// 3. 判断是否正常断开
		boolean isNotNormalDisconnect = context.get(MqttConst.DIS_CONNECTED) == null;
		if (isNotNormalDisconnect || throwable != null) {
			logger.error("Mqtt server close clientId:{}, remark:{} isRemove:{}", clientId, remark, isRemove, throwable);
		} else {
			logger.info("Mqtt server close clientId:{} remark:{} isRemove:{}", clientId, remark, isRemove);
		}
		// 4. 业务 id 不能为空
		if (StrUtil.isBlank(clientId)) {
			return;
		}
		// 5. 对于异常断开连接，处理遗嘱消息
		if (isNotNormalDisconnect) {
			sendWillMessage(clientId);
		}
		// 6. 会话清理
		cleanSession(clientId);
		// 7. 解绑 clientId
		Tio.unbindBsId(context);
		// 8. 下线事件
		notify(clientId);
	}

	private void sendWillMessage(String clientId) {
		// 发送遗嘱消息
		try {
			Message willMessage = messageStore.getWillMessage(clientId);
			if (willMessage == null) {
				return;
			}
			boolean result = messageDispatcher.send(willMessage);
			logger.info("Mqtt server clientId:{} send willMessage result:{}.", clientId, result);
			// 4. 清理遗嘱消息
			messageStore.clearWillMessage(clientId);
		} catch (Throwable throwable) {
			logger.error("Mqtt server clientId:{} send willMessage error.", clientId, throwable);
		}
	}

	private void cleanSession(String clientId) {
		try {
			sessionManager.remove(clientId);
		} catch (Throwable throwable) {
			logger.error("Mqtt server clientId:{} session clean error.", clientId, throwable);
		}
	}

	private void notify(String clientId) {
		try {
			connectStatusListener.offline(clientId);
		} catch (Throwable throwable) {
			logger.error("Mqtt server clientId:{} offline notify error.", clientId, throwable);
		}
	}

	@Override
	public void onAfterSent(ChannelContext context, Packet packet, boolean isSentSuccess) {
		if (!(packet instanceof HttpResponse)) {
			return;
		}
		// 1. 短链接数据解绑
		TioConfig tioConfig = context.getTioConfig();
		tioConfig.groups.unbind(context);
		tioConfig.bsIds.unbind(context);
		tioConfig.ids.unbind(context);
		tioConfig.clientNodes.remove(context);
		tioConfig.tokens.unbind(context);
		// 2. 关闭
		HttpResponse httpResponse = (HttpResponse) packet;
		HttpRequest request = httpResponse.getHttpRequest();
		if (request != null) {
			if (request.httpConfig.compatible1_0) {
				if (HttpConst.HttpVersion.V1_0.equals(request.requestLine.version)) {
					if (!HttpConst.RequestHeaderValue.Connection.keep_alive.equals(request.getConnection())) {
						Tio.remove(context, "http 请求头Connection!=keep-alive：" + request.getRequestLine());
					}
				} else {
					if (HttpConst.RequestHeaderValue.Connection.close.equals(request.getConnection())) {
						Tio.remove(context, "http 请求头Connection=close：" + request.getRequestLine());
					}
				}
			} else {
				if (HttpConst.RequestHeaderValue.Connection.close.equals(request.getConnection())) {
					Tio.remove(context, "http 请求头Connection=close：" + request.getRequestLine());
				}
			}
		}
	}

}
