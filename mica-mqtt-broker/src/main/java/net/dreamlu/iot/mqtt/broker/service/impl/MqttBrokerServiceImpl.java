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

package net.dreamlu.iot.mqtt.broker.service.impl;

import net.dreamlu.iot.mqtt.broker.enums.RedisKeys;
import net.dreamlu.iot.mqtt.broker.model.ServerNode;
import net.dreamlu.iot.mqtt.broker.service.IMqttBrokerService;
import net.dreamlu.mica.core.utils.StringPool;
import net.dreamlu.mica.redis.cache.MicaRedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * mqtt broker 服务
 *
 * @author L.cm
 */
@Service
public class MqttBrokerServiceImpl implements IMqttBrokerService {
	@Autowired
	private MicaRedisCache redisCache;

	@Override
	public List<ServerNode> getNodes() {
		Set<ServerNode> nodeSet = redisCache.sMembers(RedisKeys.SERVER_NODES.getKey());
		if (nodeSet == null || nodeSet.isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(nodeSet);
	}

	@Override
	public List<String> getOnlineClients() {
		Set<String> keySet = redisCache.scan(RedisKeys.CONNECT_STATUS.getKey(StringPool.STAR));
		if (keySet.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> clientList = new ArrayList<>();
		for (String redisKey : keySet) {
			Set<String> members = redisCache.sMembers(redisKey);
			if (members != null && !members.isEmpty()) {
				clientList.addAll(members);
			}
		}
		return clientList;
	}

	@Override
	public List<String> getOnlineClients(String nodeName) {
		Set<String> members = redisCache.sMembers(RedisKeys.CONNECT_STATUS.getKey(nodeName));
		if (members == null || members.isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(members);
	}

}
