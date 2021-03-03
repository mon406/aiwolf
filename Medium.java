/**
 * Medium.java
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
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * ��}�t�G�[�W�F���g
 * 
 * @author mon25
 *
 */
public class Medium extends Villager {

	private int coDate = 3; // CO�\���
	private boolean foundWolf; // �l�T�𔭌�������
	private boolean hasCO; // CO�ςȂ�true
	private Deque<Judge> myJudgeQueue = new LinkedList<>(); // ��}���ʂ̑҂��s��
	
	private List<Agent> OtherMedium = new ArrayList<>(); // ���̗�}�t
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		foundWolf = false;
		hasCO = false;
		myJudgeQueue.clear();
		OtherMedium.clear();
	}
	
	@Override
	public void dayStart() {
		super.dayStart();
		// ��}���ʂ�҂��s��ɓ����
		Judge judge = gameInfo.getMediumResult();
		if (judge != null) {
			myJudgeQueue.offer(judge);
			if (judge.getResult() == Species.WEREWOLF) {
				foundWolf = true;
			}
		}
	}
	
	@Override
	public String talk() {
		List<Agent> candidates = new ArrayList<>(); // ���[����
		
		// �\������邢�͑��̗�}�t��CO���邢�͐l�T�𔭌�������CO
		List<Agent> otherMedium = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.MEDIUM)
				.collect(Collectors.toList());
		OtherMedium = getAlive(otherMedium);
		if (!hasCO && (gameInfo.getDay() == coDate || !OtherMedium.isEmpty() || foundWolf)) {
			hasCO = true;
			return new Content(new ComingoutContentBuilder(me, me, Role.MEDIUM)).getText();
		}
		
		// CO��͗�\�s�g���ʂ��
		if (hasCO && !myJudgeQueue.isEmpty()) {
			Judge judge = myJudgeQueue.poll();
			return new Content(new IdentContentBuilder(me, judge.getTarget(), judge.getResult())).getText();
		}
		
		// �U�肢�t
		List<Agent> fakeSeers = divinationReports.stream()
				.filter(j -> j.getResult() == Species.WEREWOLF && j.getTarget() == me).map(j -> j.getAgent()).distinct()
				.collect(Collectors.toList());
		if(!fakeSeers.isEmpty()) {
			for(int i = 0; i < fakeSeers.size(); i++) {
				double trust_temp = trust.get(fakeSeers.get(i));
				trust_temp = trust_temp - 0.2;
				if(trust_temp < 0) { trust_temp = 0; }
				trust.replace(fakeSeers.get(i), trust_temp);
			}
		}
		// �U��}�t�ɓ��[
		List<Agent> fakeMedium = comingoutMap.keySet().stream()
				.filter(a -> isAlive(a) && comingoutMap.get(a) == Role.MEDIUM).collect(Collectors.toList());
		if(!fakeMedium.isEmpty()) {
			for(int i = 0; i < fakeMedium.size(); i++) {
				trust.replace(fakeMedium.get(i), 0.1);
			}
		}
		
		// �肢�t���E���ꂽ�ꍇ�A�c��̐肢�t�̐M���x��������
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
		// �E���ꂽ�肢�t�̐肢���ʂ̐M���x���オ��
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
		
		// �肢�t����l�T�Ɣ��肳�ꂽ�����G�[�W�F���g�̐M���x��������
		List<Agent> likeWerewolf = getOthers(getAlive(divinationReports.stream()
					.filter(j -> !fakeSeers.contains(j.getAgent()) && j.getResult() == Species.WEREWOLF)
					.map(j -> j.getTarget()).distinct().collect(Collectors.toList())));
		if(!likeWerewolf.isEmpty()) {
			for(int i = 0; i < likeWerewolf.size(); i++) {
				double trust_temp = trust.get(likeWerewolf.get(i));
				trust_temp = trust_temp - 0.05;
				if(trust_temp < 0) { trust_temp = 0; }
				trust.replace(likeWerewolf.get(i), trust_temp);
			}
		}
		
		// ���Ȃ���ΐ����U��}�t�ɓ��[
		if (candidates.isEmpty()) {
			candidates = getAlive(fakeMedium);
			
			// �U��}�t�̐M���x��������
			if(!fakeMedium.isEmpty()) {
				for(int i = 0; i < identificationReports.size(); i++) {
					Judge judge_result = identificationReports.get(i);
					for(int j = 0; j < fakeMedium.size(); j++) {
						if(judge_result.getAgent() == fakeMedium.get(j)) {
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

}
