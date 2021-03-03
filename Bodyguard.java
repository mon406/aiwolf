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
 * ��l�G�[�W�F���g
 * 
 * @author mon25
 *
 */
public class Bodyguard extends Villager {
	private boolean hasCO; // CO�ςȂ�true
	private boolean hasCO2; // ��q�����񍐍ςȂ�true

	private Agent toBeGuarded; // ��q�Ώ�
	private List<Agent> OtherBodyguard = new ArrayList<>(); // ���̎�l
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		toBeGuarded = null;
		OtherBodyguard.clear();
	}
	
	@Override
	public Agent guard() {
		// �M���x�̍����l������q
		List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
		List<Agent> untrust_agent = new ArrayList<Agent>();  // �M���x���ł������l��
		double most_untrust = 0.5; // �M���x���ł������l���̐M���x
		for(int i = 0; i < agent_alive.size(); i++) {
			double now_trust = trust.get(agent_alive.get(i));
			if(untrust_agent.isEmpty() || now_trust >= most_untrust) {
				if(now_trust > most_untrust) { untrust_agent.clear(); }
				untrust_agent.add(agent_alive.get(i));
				most_untrust = now_trust;
			}
		}
		List<Agent> candidates = untrust_agent;
		
		// ���񂠂邢�͕ύX����̏ꍇ�C��q����X�V
		if (toBeGuarded == null || !candidates.contains(toBeGuarded)) {
			toBeGuarded = randomSelect(candidates);
		}
		
		return toBeGuarded;
	}
	
	@Override
	public String talk() {
		super.talk();
		
		// �U���R�m��CO
		List<Agent> otherBodyguard = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.BODYGUARD)
				.collect(Collectors.toList());
		OtherBodyguard = getAlive(otherBodyguard);
		if (!hasCO && !OtherBodyguard.isEmpty()) {
			hasCO = true;
			return new Content(new ComingoutContentBuilder(me, me, Role.BODYGUARD)).getText();
		}
		
		// ��q�����������ꍇ
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
