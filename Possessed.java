/* Possessed.java
 *
 */
package jp.ac.yamagata_u.st.momma.aiwolf;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 狂人エージェント(占い師エージェントより)
 * 
 * @author mon25
 *
 */
public class Possessed extends Villager {

	private int coDate = 3; // CO予定日
	private boolean hasCO; // CO済ならtrue
	private Deque<Judge> myJudgeQueue = new LinkedList<>(); // 占い結果の待ち行列
	private List<Agent> notDivinedAgents = new ArrayList<>(); // 未占いエージェント
	
	private List<Agent> OtherSeer = new ArrayList<>(); // 他の占い師
	private int werewolf_number = 0; // 自信が人狼と占った人数
	private int default_werewolf_number = 0;
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		default_werewolf_number = gameSetting.getRoleNum(Role.WEREWOLF);
		hasCO = false;
		myJudgeQueue.clear();
		notDivinedAgents = getOthers(gameInfo.getAgentList());
		OtherSeer.clear();
		werewolf_number = 0;
	}
	
	@Override
	public void dayStart() {
		super.dayStart();
		
		// まだ占っていない生存エージェントからランダムに占う
		List<Agent> nondivinedAgent = new ArrayList<>();
		nondivinedAgent = getAlive(notDivinedAgents);
		notDivinedAgents.clear();
		notDivinedAgents = nondivinedAgent;
		Agent divined_agent = randomSelect(notDivinedAgents);
		if(divined_agent != null) {
			// 信頼度によって人か人狼か決める
			double trustLEVEL = trust.get(divined_agent);
			Species feelResult;
			if(trustLEVEL > 0.35) {
				feelResult = Species.HUMAN;
			}
			else if(werewolf_number >= default_werewolf_number) {
				feelResult = Species.HUMAN;
			}
			else { 
				feelResult = Species.WEREWOLF;
				werewolf_number++;
			}
			// 偽占い結果の処理
			Judge judge = new Judge(gameInfo.getDay() - 1, me, divined_agent, feelResult);
			if (judge != null) {
				myJudgeQueue.offer(judge);
				notDivinedAgents.remove(judge.getTarget());
			}
		}
	}
	
	@Override
	public String talk() {
		// 予定日あるいは他の占い師がCOしたらCO
		List<Agent> candidates = new ArrayList<>(); // 投票先候補
		
		List<Agent> otherSeer = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.SEER)
				.collect(Collectors.toList());
		OtherSeer = getAlive(otherSeer);
		if (!hasCO && (gameInfo.getDay() == coDate || !OtherSeer.isEmpty())) {
			hasCO = true;
			return new Content(new ComingoutContentBuilder(me, me, Role.SEER)).getText();
		}
	
		// CO後は占い結果を報告
		if (hasCO && !myJudgeQueue.isEmpty()) {
			Judge judge = myJudgeQueue.poll();
			return new Content(new DivinedResultContentBuilder(me, judge.getTarget(), judge.getResult())).getText();
		}
	
		// 生存本物占い師の信頼度を上げて、投票
		if (candidates.isEmpty()) {
			candidates = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.SEER)
					.collect(Collectors.toList());
		}
		if (!candidates.isEmpty()) {
			for(int i = 0; i < candidates.size(); i++) {
				double trust_temp = trust.get(candidates.get(i));
				trust_temp = trust_temp + 0.2;
				if(trust_temp > 1) { trust_temp = 1; }
				trust.replace(candidates.get(i), trust_temp);
			}
		}
		
		// 占い師が殺された場合、残りの占い師の信頼度が下がる
		List<Agent> deadAgent = gameInfo.getLastDeadAgentList();
		List<Agent> deadSeer = new ArrayList<>();
		if (deadAgent.isEmpty() == false) {
			for(int i = 0; i < deadAgent.size(); i++) {
				Role deadAgent_role = comingoutMap.get(deadAgent.get(i));
				if(deadAgent_role == Role.SEER) {
					deadSeer.add(deadAgent.get(i));
					List<Agent> seer_index = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.SEER)
							.collect(Collectors.toList());
					if(seer_index.size() > 0) {
						double trust_temp = trust.get(seer_index.get(i));
						trust_temp = trust_temp - 0.3;
						if(trust_temp < 0) { trust_temp = 0; }
						trust.replace(seer_index.get(i), trust_temp);
					}
				}
			}
		}
		// 殺された占い師の占い結果の信頼度が上がる
		if (deadSeer.isEmpty() == false && divinationReports.isEmpty() == false) {
			for(int i = 0; i < divinationReports.size(); i++) {
				Judge judge_result = divinationReports.get(i);
				for(int j = 0; j < deadSeer.size(); j++) {
					if(judge_result.getAgent() == deadSeer.get(j)) {
						switch (judge_result.getResult()) {
							case HUMAN:
							double trust_tempHUMAN = trust.get(judge_result.getTarget());
							trust_tempHUMAN = trust_tempHUMAN + 0.1;
							if(trust_tempHUMAN > 1) { trust_tempHUMAN = 1; }
							trust.replace(judge_result.getTarget(), trust_tempHUMAN);
							break;
						case WEREWOLF:
							double trust_tempWOLF = trust.get(judge_result.getTarget());
							trust_tempWOLF = trust_tempWOLF - 0.1;
							if(trust_tempWOLF < 0) { trust_tempWOLF = 0; }
							trust.replace(judge_result.getTarget(), trust_tempWOLF);
							break;
						default:
							break;
						}
					}
				}
			}
		}
				
		// 霊媒師の結果と占いの結果が異なる占い師の信頼度が下がる
		if (identificationReports.isEmpty() == false) {
			for(int i = 0; i < identificationReports.size(); i++) {
				Judge after_judge_result = identificationReports.get(i);
				for(int j = 0; j < divinationReports.size(); j++) {
					Judge judge_result = divinationReports.get(j);
					if(after_judge_result.getTarget() == judge_result.getTarget()) {
						if(after_judge_result.getResult() != judge_result.getResult()) {
							double trust_temp = trust.get(judge_result.getAgent());
							trust_temp = trust_temp - 0.1;
							if(trust_temp < 0) { trust_temp = 0; }
							trust.replace(judge_result.getAgent(), trust_temp);
						}
					}
				}
			}
		}
		
		// 自分の得票数が最多の場合、他の得票数が多い人に投票
		List<Agent> aliveList = gameInfo.getAliveAgentList();
		Map<Agent, Integer> voteList = new HashMap<>();
		for(int i = 0; i < aliveList.size(); i++) { // 0で初期化
			voteList.put(aliveList.get(i), 0);
		}
		for(int i = 0; i < aliveList.size(); i++) {
			if(isAlive(num_voteCandidate.get(aliveList.get(i)))) {
				int vote_index = voteList.get(aliveList.get(i));
				vote_index++;
				voteList.replace(aliveList.get(i), vote_index);
			}
		}
		int first_vote = 0, second_vote = 0;
		for(int i = 0; i < aliveList.size(); i++) { // 最多得票先を取得
			int now_number = voteList.get(aliveList.get(i));
			if(now_number > second_vote) {
				second_voteCandidate = aliveList.get(i);
				second_vote = now_number;
			}
			if(second_vote > first_vote) {
				first_voteCandidate = second_voteCandidate;
				first_vote = second_vote;
			}
		}
		if(first_voteCandidate == me) {
			candidates.add(second_voteCandidate);
		}
		
		// いなければ信頼度の高い生存エージェントに投票
		if (candidates.isEmpty()) {
			List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
			List<Agent> trust_agent = new ArrayList<Agent>();  // 信頼度が最も高い人物
			double most_trust = 0.5; // 信頼度が最も高い人物の信頼度
			for(int i = 0; i < agent_alive.size(); i++) {
				double now_trust = trust.get(agent_alive.get(i));
				if(trust_agent.isEmpty() || now_trust <= most_trust) {
					if(now_trust < most_trust) { trust_agent.clear(); }
					trust_agent.add(agent_alive.get(i));
					most_trust = now_trust;
				}
			}
			agent_alive.clear();
			candidates = trust_agent;
		}
	
		// それでもいなければ生存エージェントに投票
		if (candidates.isEmpty()) {
			candidates = getOthers(gameInfo.getAliveAgentList());
		}
	
		// 初めての投票先宣言あるいは変更ありの場合，投票先宣言
		if (voteCandidate == null || !candidates.contains(voteCandidate)) {
			voteCandidate = randomSelect(candidates);
			return new Content(new VoteContentBuilder(me, voteCandidate)).getText();
		}
	
		return Talk.SKIP;
	}

}
