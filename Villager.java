/**
 * Villager.java
 */
package jp.ac.yamagata_u.st.momma.aiwolf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * ���l�G�[�W�F���g
 * 
 * @author mon25
 *
 */
public class Villager implements Player {

	protected Agent me; // ����
	protected Agent voteCandidate; // ���[��
	protected GameInfo gameInfo; // �Q�[�����
	protected Map<Agent, Role> comingoutMap = new HashMap<>(); // �J�~���O�A�E�g��
	protected List<Judge> divinationReports = new ArrayList<>(); // �肢���ʕ񍐎��n��
	protected List<Judge> identificationReports = new ArrayList<>(); // ��}���ʕ񍐎��n��
	
	private int talkListHead; // ����͉�b�̐擪�C���f�b�N�X
	
	protected Map<Agent, Double> trust = new HashMap<>(); // ����l���ɑ΂���M���x�i0:���l�w�c-1:�l�T�w�c�j
	protected Map<Agent, Agent> num_voteCandidate = new HashMap<>(); // �����ȊO�̓��[��
	protected Agent first_voteCandidate; // �ő����[��
	protected Agent second_voteCandidate; // �Q�Ԗڂ̍ő����[��
	
	/**
	 * �G�[�W�F���g�������Ă��邩�ǂ���
	 *
	 * @param agent �G�[�W�F���g
	 * @return �����Ă����true
	 */	
	protected boolean isAlive(Agent agent) {
		return gameInfo.getStatusMap().get(agent) == Status.ALIVE;
	}
	
	/**
	 * �G�[�W�F���g���X�g���玩�������������X�g��Ԃ�
	 *
	 * @param agentList �G�[�W�F���g�̃��X�g
	 * @return �G�[�W�F���g�̃��X�g
	 */
	protected List<Agent> getOthers(List<Agent> agentList) {
		return agentList.stream().filter(a -> a != me).collect(Collectors.toList());
	}
	
	/**
	 * �G�[�W�F���g���X�g���̐����G�[�W�F���g�̃��X�g��Ԃ�
	 *
	 * @param agentList �G�[�W�F���g�̃��X�g
	 * @return �G�[�W�F���g�̃��X�g
	 */
	protected List<Agent> getAlive(List<Agent> agentList) {
		return agentList.stream().filter(a -> isAlive(a)).collect(Collectors.toList());
	}
	
	/**
	 * �G�[�W�F���g�̃��X�g���烉���_����1�G�[�W�F���g��I��
	 *
	 * @param agentList �G�[�W�F���g�̃��X�g
	 * @return ���X�g����̏ꍇnull
	 */
	protected Agent randomSelect(List<Agent> agentList) {
		return agentList.isEmpty() ? null : agentList.get((int) (Math.random() * agentList.size()));
	}
	
	@Override
	public String getName() {
		return "Momma";
	}
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		this.gameInfo = gameInfo;
		me = gameInfo.getAgent();
		// �O�̃Q�[������������Ȃ��悤�Ƀt�B�[���h���N���A���Ă���
		comingoutMap.clear();
		divinationReports.clear();
		identificationReports.clear();
		trust.clear();
		
		// �M���x�̏�����
		List<Agent> agent_all = gameInfo.getAgentList();
		for(int i = 0; i < agent_all.size(); i++) {
			trust.put(agent_all.get(i), 0.5); // �S�ẴG�[�W�F���g�ɑ΂���0.5�ŏ�����
		}
		agent_all.clear();
	}
	
	@Override
	public void dayStart() {
		talkListHead = 0;
		voteCandidate = null;
		
		num_voteCandidate.clear();
		first_voteCandidate = null;
		second_voteCandidate = null;
	}
	
	@Override
	public void update(GameInfo gameInfo) {
		this.gameInfo = gameInfo; // �Q�[���󋵍X�V
		for (int i = talkListHead; i < gameInfo.getTalkList().size(); i++) { // ����͉�b�̉��
			Talk talk = gameInfo.getTalkList().get(i); // ��͑Ώۉ�b
			Agent talker = talk.getAgent(); // ���������G�[�W�F���g
			if (talker == me) {// �����̔����͉�͂��Ȃ�
				continue;
			}
			// ��b�����񂻂̂��̂���Content���쐬���������������₷��
			Content content = new Content(talk.getText());
			switch (content.getTopic()) {
			case VOTE:
				num_voteCandidate.put(talker, content.getTarget()); // �����ȊO�̓��[���c��
				break;
			case COMINGOUT:
				comingoutMap.put(talker, content.getRole());
				break;
			case DIVINED:
				divinationReports.add(new Judge(gameInfo.getDay(), talker, content.getTarget(), content.getResult()));
				break;
			case IDENTIFIED:
				identificationReports
				.add(new Judge(gameInfo.getDay(), talker, content.getTarget(), content.getResult()));
				break;
			default:
				break;
			}
		}
	talkListHead = gameInfo.getTalkList().size(); // ���ׂĉ�͍ς݂Ƃ���
	}
	
	@Override
	public String talk() {
		// ��b�����Ȃ��瓊�[������߂Ă���
		List<Agent> candidates = new ArrayList<>(); // ���[����
	
		// �����i���l�j��l�T�Ɣ��肵���U�肢�t�̃��X�g
		List<Agent> fakeSeers = divinationReports.stream()
				.filter(j -> j.getResult() == Species.WEREWOLF && j.getTarget() == me).map(j -> j.getAgent()).distinct()
				.collect(Collectors.toList());
		for(int i = 0; i < fakeSeers.size(); i++) {
			trust.replace(fakeSeers.get(i), 0.1); // ������l�T���肵���l�͂قڊm���ɐl�T�w�c
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
		
		// ��}�t�̌��ʂƐ肢�̌��ʂ��قȂ�肢�t�̐M���x��������
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
		
		// �����U�肢�t�ɓ��[
		if (candidates.isEmpty()) {
			candidates = getAlive(fakeSeers);
		}
		
		// �U�łȂ����̐肢�t����l�T�Ɣ��肳�ꂽ�����G�[�W�F���g�̐M���x��������
		List<Agent> candidates_index = getOthers(getAlive(divinationReports.stream()
				.filter(j -> !fakeSeers.contains(j.getAgent()) && j.getResult() == Species.WEREWOLF)
				.map(j -> j.getTarget()).distinct().collect(Collectors.toList())));
		for(int i = 0; i < candidates_index.size(); i++) {
			double trust_temp = trust.get(candidates_index.get(i));
			trust_temp = trust_temp - 0.1;
			if(trust_temp < 0) { trust_temp = 0; }
			trust.replace(candidates_index.get(i), trust_temp);
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
	public Agent vote() {
	return voteCandidate;
	}
	
	@Override
	public Agent attack() {
	throw new UnsupportedOperationException(); // ��g�p�̏ꍇ��O���o
	}
	
	@Override
	public Agent divine() {
	throw new UnsupportedOperationException(); // ��g�p�̏ꍇ��O���o
	}
	
	@Override
	public Agent guard() {
	throw new UnsupportedOperationException(); // ��g�p�̏ꍇ��O���o
	}
	
	@Override
	public String whisper() {
	throw new UnsupportedOperationException(); // ��g�p�̏ꍇ��O���o
	}
	
	@Override
	public void finish() {
	}

}
