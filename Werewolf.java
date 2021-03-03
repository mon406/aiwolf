/* Werewolf.java
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
//import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import java.util.Random;

/**
 * �l�T�G�[�W�F���g
 * 
 * @author mon25
 *
 */
public class Werewolf implements Player {
	private int coDate = 3; // �U��}�tCO�\���
	private boolean hasCO; // �U��}�tCO�ςȂ�true
	private boolean foundWolf; // �l�T�𔭌�������
	private List<Agent> OtherMedium = new ArrayList<>(); // ���̗�}�t
	private Deque<Judge> myJudgeQueue = new LinkedList<>(); // ��}���ʂ̑҂��s��
	
	private int werewolf_number = 0; // ���M���l�T�Ɨ�}�����l��
	private int default_werewolf_number = 0;
	
	
	protected Agent voteTarget; // �P����
	private List<Agent> OtherWerewolf = new ArrayList<>(); // ���̐l�T
	private List<Agent> Other_voteTarget = new ArrayList<>(); // ���̐l�T�̏P����
	private int whisper_index = 0;
	
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
		OtherWerewolf.clear();
		
		OtherMedium.clear();
		myJudgeQueue.clear();
		foundWolf = false;
		werewolf_number = 0;
		default_werewolf_number = gameSetting.getRoleNum(Role.WEREWOLF);
		
		// �M���x�̏�����
		List<Agent> agent_all = gameInfo.getAgentList();
		for(int i = 0; i < agent_all.size(); i++) {
			trust.put(agent_all.get(i), 0.5); // �S�ẴG�[�W�F���g�ɑ΂���0.5�ŏ�����
		}
		agent_all.clear();
	}

	@Override
	public Agent attack() {
		// �P���悪�Ǖ����ꂽ�ꍇ
		if(!isAlive(voteTarget)) {
			List<Agent> target = getOthers(gameInfo.getAliveAgentList());
			voteTarget = randomSelect(target);
		}
		
		return voteTarget; // �P��
	}
	
	@Override
	public void dayStart() {
		talkListHead = 0;
		voteCandidate = null;
		voteTarget = null;
		Other_voteTarget.clear();
		
		whisper_index = 0;
		num_voteCandidate.clear();
		first_voteCandidate = null;
		second_voteCandidate = null;
		
		// �M���x�ɂ���Đl���l�T�����߂�
		Agent ExecutedAgent = gameInfo.getExecutedAgent();
		if(ExecutedAgent != null) {
			double trustLEVEL = trust.get(ExecutedAgent);
			Species feelResult;
			if(trustLEVEL <= 0.8) {
				feelResult = Species.HUMAN;
			}
			else if(werewolf_number < (default_werewolf_number - 1)) {
				feelResult = Species.WEREWOLF;
				werewolf_number++;
			}
			else { 
				feelResult = Species.HUMAN;
			}
			// ��}���ʂ�҂��s��ɓ����
			Judge judge = new Judge(gameInfo.getDay() - 1, me, ExecutedAgent, feelResult);
			if (judge != null) {
				myJudgeQueue.offer(judge);
				if (judge.getResult() == Species.WEREWOLF) {
					foundWolf = true;
				}
			}
		}
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
			case ATTACK:
				Other_voteTarget.add(content.getTarget());
				break;
			default:
				break;
			}
		}
	talkListHead = gameInfo.getTalkList().size(); // ���ׂĉ�͍ς݂Ƃ���
	}
	
	@Override
	public String talk() {
		// ��b�����Ȃ��瓊�[��ƏP��������߂Ă���
		List<Agent> target = getOthers(gameInfo.getAliveAgentList()); // �P������
		List<Agent> candidates = getOthers(gameInfo.getAliveAgentList()); // ���[����
		
		// �\����ȍ~�ɑ��̗�}�t����l�ȉ��̂Ƃ��m���I��CO
		List<Agent> otherMedium = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.MEDIUM)
				.collect(Collectors.toList());
		OtherMedium = getAlive(otherMedium);
		if (!hasCO && (gameInfo.getDay() >= coDate || OtherMedium.size() <= 1 || foundWolf)) {
			Random rand = new Random();
		    int num = rand.nextInt(10);
		    if(gameInfo.getDay() > 3) {
		    	num = num + (gameInfo.getDay() - 3);
		    }
			if(num < 5) {
				hasCO = true;
				return new Content(new ComingoutContentBuilder(me, me, Role.MEDIUM)).getText();
			}
		}
		if(!otherMedium.isEmpty()) {
			for(int i = 0; i < otherMedium.size(); i++) {
				double trust_temp = trust.get(otherMedium.get(i));
				trust_temp = trust_temp + 0.25;
				if(trust_temp > 1) { trust_temp = 1; }
				trust.replace(otherMedium.get(i), trust_temp);
			}
		}
		
		// CO��͗�\�s�g���ʂ��
		if (hasCO && !myJudgeQueue.isEmpty()) {
			Judge judge = myJudgeQueue.poll();
			return new Content(new IdentContentBuilder(me, judge.getTarget(), judge.getResult())).getText();
		}
		
		// �����i���l�j��l�T�Ɣ��肵���肢�t�̃��X�g
		List<Agent> RealSeers = divinationReports.stream()
				.filter(j -> j.getResult() == Species.WEREWOLF && j.getTarget() == me).map(j -> j.getAgent()).distinct()
				.collect(Collectors.toList());
		for(int i = 0; i < RealSeers.size(); i++) {
			trust.replace(RealSeers.get(i), 0.9); // ������l�T���肵���l�͂قڊm���ɑ��l�w�c
		}
		// �����i���l�j�𑺐l�Ɣ��肵���肢�t�̃��X�g
		List<Agent> FeelSeers = divinationReports.stream()
				.filter(j -> j.getResult() == Species.HUMAN && j.getTarget() == me).map(j -> j.getAgent()).distinct()
				.collect(Collectors.toList());
		for(int i = 0; i < FeelSeers.size(); i++) {
			trust.replace(FeelSeers.get(i), 0.1); // ������l�T���肵���l�͂قڊm���ɐl�T�w�c
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
		
		// �����{���肢�t�ɓ��[
		if (candidates.isEmpty()) {
			candidates = getAlive(RealSeers);
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
		
		// �M���x�̍��������G�[�W�F���g�ɓ��[�܂��͏P��
		if (candidates.isEmpty()) {
			// ���̐M���x�̍����l�����P��
			List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
			List<Agent> trust_agent = new ArrayList<Agent>();  // �M���x���ł������l��
			double most_trust = 0.5; // �M���x���ł������l���̐M���x
			for(int j = 0; j < agent_alive.size(); j++) {
				double now_trust = trust.get(agent_alive.get(j));
				if(trust_agent.isEmpty() || now_trust >= most_trust) {
					if(now_trust > most_trust) { trust_agent.clear(); }
					trust_agent.add(agent_alive.get(j));
					most_trust = now_trust;
				}
			}
			agent_alive.clear();
			candidates = trust_agent;
		}
		if (target.isEmpty()) {
			// �肢�t����l�̏ꍇ�͎����𑺐l�ƌ����Ă��Ȃ�����P��
			List<Agent> aliveSeer = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.SEER)
					.collect(Collectors.toList());
			if(aliveSeer.size() == 1) {
				target.add(aliveSeer.get(0));
			}
			
			// �肢�t�ȊO�ň�l�����J�~���O�A�E�g���Ă��Ȃ���E�̐l���͐M���x���オ��
			List<Agent> aliveMedium = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.MEDIUM)
					.collect(Collectors.toList());
			if(aliveMedium.size() == 1) {
				target.add(aliveMedium.get(0));
				double trust_temp = trust.get(aliveMedium.get(0));
				trust_temp = trust_temp + 0.1;
				if(trust_temp > 1) { trust_temp = 1; }
				trust.replace(aliveMedium.get(0), trust_temp);
			}
			List<Agent> aliveBodyguard = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.BODYGUARD)
					.collect(Collectors.toList());
			if(aliveBodyguard.size() == 1) {
				target.add(aliveBodyguard.get(0));
				double trust_temp = trust.get(aliveBodyguard.get(0));
				trust_temp = trust_temp + 0.2;
				if(trust_temp > 1) { trust_temp = 1; }
				trust.replace(aliveBodyguard.get(0), trust_temp);
			}
		}
		if (target.isEmpty()) {	
			// ���̐M���x�̍����l�����P��
			List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
			List<Agent> trust_agent = new ArrayList<Agent>();  // �M���x���ł������l��
			double most_trust = 0.5; // �M���x���ł������l���̐M���x
			for(int j = 0; j < agent_alive.size(); j++) {
				double now_trust = trust.get(agent_alive.get(j));
				if(trust_agent.isEmpty() || now_trust >= most_trust) {
					if(now_trust > most_trust) { trust_agent.clear(); }
					trust_agent.add(agent_alive.get(j));
					most_trust = now_trust;
				}
			}
			agent_alive.clear();
			target = trust_agent;
		}
				
		// ����ł����Ȃ���ΐ����G�[�W�F���g�ɓ��[�܂��͏P��
		if (target.isEmpty()) {
			target = getOthers(gameInfo.getAliveAgentList());
		}
		if (candidates.isEmpty()) {
			candidates = getOthers(gameInfo.getAliveAgentList());
			if(!OtherWerewolf.isEmpty()) {
				int k = 0;
				for(int i = 0; i < OtherWerewolf.size(); i++) {
				while(candidates.contains(OtherWerewolf.get(i)) && k < 10) {
					candidates = getOthers(gameInfo.getAliveAgentList());
					k++;
				}
				}
			}
		}
	
		// ���[��ƏP����
		if (voteCandidate == null || !target.contains(voteCandidate)) {
			voteTarget = randomSelect(target);
		}
		// ���߂Ă̓��[��錾���邢�͕ύX����̏ꍇ�C���[��錾
		if (voteCandidate == null || !candidates.contains(voteCandidate)) {
			voteCandidate = randomSelect(candidates);
			return new Content(new VoteContentBuilder(me, voteCandidate)).getText();
		}
		
		return Talk.SKIP; // �l�q��
	}
	
	@Override
	public Agent vote() {
		return voteCandidate;
	}

	@Override
	public String whisper() {
		// ������ʂ��čŏI�I�ȏP��������߂Ă���
		if(whisper_index == 0) {
			whisper_index++;
			return new Content(new ComingoutContentBuilder(me, Role.WEREWOLF)).getText();
		}
		
		// ���̐l�T���J�~���O�A�E�g�����Ƃ�
		OtherWerewolf = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.WEREWOLF)
				.collect(Collectors.toList());
		
		// �P���悪������ꍇ
		if(Other_voteTarget.size() > 0) {
			for(int i = 0; i < Other_voteTarget.size(); i++) {
				if(voteTarget == Other_voteTarget.get(i)) {
					// ���̐M���x�̍����l�����P��
					List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
					List<Agent> trust_agent = new ArrayList<Agent>();  // �M���x���ł������l��
					double most_trust = 0.5; // �M���x���ł������l���̐M���x
					for(int j = 0; j < agent_alive.size(); j++) {
						double now_trust = trust.get(agent_alive.get(j));
						if(trust_agent.isEmpty() || now_trust >= most_trust) {
							if(now_trust > most_trust) { trust_agent.clear(); }
							trust_agent.add(agent_alive.get(j));
							most_trust = now_trust;
						}
					}
					voteTarget = randomSelect(trust_agent);
					
					// ���Ȃ���΃����_���ɏP��
					if (trust_agent.isEmpty()) {
						voteTarget = randomSelect(agent_alive);
					}
					agent_alive.clear();
					//return new Content(new AttackContentBuilder(me, voteTarget)).getText();
				}
			}
		}
		
		return Talk.SKIP; // �l�q��
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
	public void finish() {
	}

}
