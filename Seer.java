/**
 * Seer.java
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
 * �肢�t�G�[�W�F���g
 * 
 * @author mon25
 *
 */
public class Seer extends Villager {

	private int coDate = 3; // CO�\���
	private boolean hasCO; // CO�ςȂ�true
	private Deque<Judge> myJudgeQueue = new LinkedList<>(); // �肢���ʂ̑҂��s��
	private List<Agent> notDivinedAgents = new ArrayList<>(); // ���肢�G�[�W�F���g
	private List<Agent> werewolves = new ArrayList<Agent>(); // �������l�T
	
	private List<Agent> OtherSeer = new ArrayList<>(); // ���̐肢�t
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		hasCO = false;
		myJudgeQueue.clear();
		notDivinedAgents = getOthers(gameInfo.getAgentList());
		werewolves.clear();
		OtherSeer.clear();
	}
	
	@Override
	public void dayStart() {
		super.dayStart();
		// �肢���ʂ̏���
		Judge judge = gameInfo.getDivineResult();
		if (judge != null) {
			myJudgeQueue.offer(judge);
			notDivinedAgents.remove(judge.getTarget());
			if (judge.getResult() == Species.WEREWOLF) {
				werewolves.add(judge.getTarget());
			}
		}
	}
	
	@Override
	public String talk() {
		// �\������邢�͑��̐肢�t��CO���邢�͐l�T�𔭌�������CO
		List<Agent> otherSeer = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.SEER)
				.collect(Collectors.toList());
		OtherSeer = getAlive(otherSeer);
		if (!hasCO && (gameInfo.getDay() == coDate || !OtherSeer.isEmpty() || !werewolves.isEmpty())) {
			hasCO = true;
			return new Content(new ComingoutContentBuilder(me, me, Role.SEER)).getText();
		}
	
		// CO��͐肢���ʂ��
		if (hasCO && !myJudgeQueue.isEmpty()) {
			Judge judge = myJudgeQueue.poll();
			return new Content(new DivinedResultContentBuilder(me, judge.getTarget(), judge.getResult())).getText();
		}
		
		// �����l�T�ɓ��[
		List<Agent> candidates = getAlive(werewolves);
		for(int i = 0; i < werewolves.size(); i++) {
			trust.replace(werewolves.get(i), 0.0);
		}
	
		// ���Ȃ���ΐ����U�肢�t�̐M���x��������
		List<Agent> fakeSeers = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.SEER)
					.collect(Collectors.toList());
		if(!fakeSeers.isEmpty()) {
			for(int i = 0; i < fakeSeers.size(); i++) {
				double trust_temp = trust.get(fakeSeers.get(i));
				trust_temp = trust_temp - 0.2;
				if(trust_temp < 0) { trust_temp = 0; }
				trust.replace(fakeSeers.get(i), trust_temp);
			}
			
			// �U�肢�t�̐肢���ʂ��R���ƍl����
			for(int i = 0; i < divinationReports.size(); i++) {
				Judge judge_result = divinationReports.get(i);
				for(int j = 0; j < fakeSeers.size(); j++) {
					if(judge_result.getAgent() == fakeSeers.get(j)) {
						switch (judge_result.getResult()) {
						case WEREWOLF:
							double trust_tempHUMAN = trust.get(judge_result.getTarget());
							trust_tempHUMAN = trust_tempHUMAN + 0.15;
							if(trust_tempHUMAN > 1) { trust_tempHUMAN = 1; }
							trust.replace(judge_result.getTarget(), trust_tempHUMAN);
							break;
						case HUMAN:
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
		
		// �����̐肢�ƈقȂ��}���ʂ��o������}�t�̐M���x��������
		if (identificationReports.isEmpty() == false) {
			for(int i = 0; i < identificationReports.size(); i++) {
				Judge after_judge_result = identificationReports.get(i);
				for(int j = 0; j < divinationReports.size(); j++) {
					Judge judge_result = divinationReports.get(j);
					if(judge_result.getAgent() == me && after_judge_result.getTarget() == judge_result.getTarget()) {
						if(after_judge_result.getResult() != judge_result.getResult()) {
							double trust_temp = trust.get(judge_result.getAgent());
							trust_temp = trust_temp - 0.25;
							if(trust_temp < 0) { trust_temp = 0; }
							trust.replace(judge_result.getAgent(), trust_temp);
						}
					}
				}
			}
		}
		
		// �����̓��[�����ő��̏ꍇ�A���̓��[���������l�ɓ��[
		List<Agent> aliveList = gameInfo.getAliveAgentList();
		Map<Agent, Integer> voteList = new HashMap<>();
		for(int i = 0; i < aliveList.size(); i++) { // 0�ŏ�����
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
		for(int i = 0; i < aliveList.size(); i++) { // �ő����[����擾
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
				
		// ���Ȃ���ΐM���x�̒Ⴂ�����G�[�W�F���g�ɓ��[
		if (candidates.isEmpty()) {
			List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
			List<Agent> untrust_agent = new ArrayList<Agent>();  // �M���x���ł��Ⴂ�l��
			double most_untrust = 0.5; // �M���x���ł��Ⴂ�l���̐M���x
			for(int i = 0; i < agent_alive.size(); i++) {
				double now_trust = trust.get(agent_alive.get(i));
				if(untrust_agent.isEmpty() || now_trust <= most_untrust) {
					if(now_trust < most_untrust) { untrust_agent.clear(); }
					untrust_agent.add(agent_alive.get(i));
					most_untrust = now_trust;
				}
			}
			agent_alive.clear();
			candidates = untrust_agent;
		}
	
		// ����ł����Ȃ���ΐ����G�[�W�F���g�ɓ��[
		if (candidates.isEmpty()) {
			candidates = getOthers(gameInfo.getAliveAgentList());
		}
	
		// ���߂Ă̓��[��錾���邢�͕ύX����̏ꍇ�C���[��錾
		if (voteCandidate == null || !candidates.contains(voteCandidate)) {
			voteCandidate = randomSelect(candidates);
			return new Content(new VoteContentBuilder(me, voteCandidate)).getText();
		}
	
		return Talk.SKIP;
	}
	
	@Override
	public Agent divine() {
		Agent nowDivinedAgents; 
		// �M���x�̒Ⴂ�G�[�W�F���g����肤
		List<Agent> untrust_agent = new ArrayList<Agent>();  // �M���x���ł��Ⴂ�l��
		double most_untrust = 0.5; // �M���x���ł��Ⴂ�l���̐M���x
		for(int i = 0; i < notDivinedAgents.size(); i++) {
			double now_trust = trust.get(notDivinedAgents.get(i));
			if(untrust_agent.isEmpty() || now_trust <= most_untrust) {
				if(now_trust < most_untrust) { untrust_agent.clear(); }
				untrust_agent.add(notDivinedAgents.get(i));
				most_untrust = now_trust;
			}
		}
		nowDivinedAgents = randomSelect(untrust_agent);
		
		// �܂�����Ă��Ȃ��G�[�W�F���g���烉���_���ɐ肤
		if(untrust_agent.isEmpty()) {
			return randomSelect(notDivinedAgents);
		}
		
		return nowDivinedAgents;
	}

}
