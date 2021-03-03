/**
 * Bodyguard.java
 *
 */
package jp.ac.yamagata_u.st.momma.aiwolf;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 狩人エージェント
 * 
 * @author mon25
 *
 */
public class Bodyguard extends Villager {
	private boolean hasCO; // CO済ならtrue
	private boolean hasCO2; // 護衛成功報告済ならtrue

	private Agent toBeGuarded; // 護衛対象
	private List<Agent> OtherBodyguard = new ArrayList<>(); // 他の狩人
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		toBeGuarded = null;
		OtherBodyguard.clear();
	}
	
	@Override
	public Agent guard() {
		// 信頼度の高い人物を護衛
		List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
		List<Agent> untrust_agent = new ArrayList<Agent>();  // 信頼度が最も高い人物
		double most_untrust = 0.5; // 信頼度が最も高い人物の信頼度
		for(int i = 0; i < agent_alive.size(); i++) {
			double now_trust = trust.get(agent_alive.get(i));
			if(untrust_agent.isEmpty() || now_trust >= most_untrust) {
				if(now_trust > most_untrust) { untrust_agent.clear(); }
				untrust_agent.add(agent_alive.get(i));
				most_untrust = now_trust;
			}
		}
		List<Agent> candidates = untrust_agent;
		
		// 初回あるいは変更ありの場合，護衛先を更新
		if (toBeGuarded == null || !candidates.contains(toBeGuarded)) {
			toBeGuarded = randomSelect(candidates);
		}
		
		return toBeGuarded;
	}
	
	@Override
	public String talk() {
		super.talk();
		
		// 偽物騎士がCO
		List<Agent> otherBodyguard = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.BODYGUARD)
				.collect(Collectors.toList());
		OtherBodyguard = getAlive(otherBodyguard);
		if (!hasCO && !OtherBodyguard.isEmpty()) {
			hasCO = true;
			return new Content(new ComingoutContentBuilder(me, me, Role.BODYGUARD)).getText();
		}
		
		// 護衛が成功した場合
		Agent LastGuarded = gameInfo.getGuardedAgent();
		if(!hasCO2 && isAlive(LastGuarded)) {
			double trust_tempB = trust.get(LastGuarded);
			trust_tempB = trust_tempB + 0.2;
			if(trust_tempB < 0) { trust_tempB = 0; }
			trust.replace(LastGuarded, trust_tempB);
			
			Role LastGuarded_role = comingoutMap.get(LastGuarded);
			if(LastGuarded_role == null) {
				LastGuarded_role = Role.VILLAGER;
			}
			
			hasCO2 = true;
			return new Content(new EstimateContentBuilder(me, LastGuarded, LastGuarded_role)).getText();
		}
		
		return Talk.SKIP;
	}

}
