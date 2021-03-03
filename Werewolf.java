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
 * 人狼エージェント
 * 
 * @author mon25
 *
 */
public class Werewolf implements Player {
	private int coDate = 3; // 偽霊媒師CO予定日
	private boolean hasCO; // 偽霊媒師CO済ならtrue
	private boolean foundWolf; // 人狼を発見したか
	private List<Agent> OtherMedium = new ArrayList<>(); // 他の霊媒師
	private Deque<Judge> myJudgeQueue = new LinkedList<>(); // 霊媒結果の待ち行列
	
	private int werewolf_number = 0; // 自信が人狼と霊媒した人数
	private int default_werewolf_number = 0;
	
	
	protected Agent voteTarget; // 襲撃先
	private List<Agent> OtherWerewolf = new ArrayList<>(); // 他の人狼
	private List<Agent> Other_voteTarget = new ArrayList<>(); // 他の人狼の襲撃先
	private int whisper_index = 0;
	
	protected Agent me; // 自分
	protected Agent voteCandidate; // 投票先
	protected GameInfo gameInfo; // ゲーム情報
	protected Map<Agent, Role> comingoutMap = new HashMap<>(); // カミングアウト状況
	protected List<Judge> divinationReports = new ArrayList<>(); // 占い結果報告時系列
	protected List<Judge> identificationReports = new ArrayList<>(); // 霊媒結果報告時系列
	
	private int talkListHead; // 未解析会話の先頭インデックス
	
	protected Map<Agent, Double> trust = new HashMap<>(); // ある人物に対する信頼度（0:村人陣営-1:人狼陣営）
	protected Map<Agent, Agent> num_voteCandidate = new HashMap<>(); // 自分以外の投票先
	protected Agent first_voteCandidate; // 最多得票先
	protected Agent second_voteCandidate; // ２番目の最多得票先
	
	/**
	 * エージェントが生きているかどうか
	 *
	 * @param agent エージェント
	 * @return 生きていればtrue
	 */	
	protected boolean isAlive(Agent agent) {
		return gameInfo.getStatusMap().get(agent) == Status.ALIVE;
	}
	
	/**
	 * エージェントリストから自分を除いたリストを返す
	 *
	 * @param agentList エージェントのリスト
	 * @return エージェントのリスト
	 */
	protected List<Agent> getOthers(List<Agent> agentList) {
		return agentList.stream().filter(a -> a != me).collect(Collectors.toList());
	}
	
	/**
	 * エージェントリスト中の生存エージェントのリストを返す
	 *
	 * @param agentList エージェントのリスト
	 * @return エージェントのリスト
	 */
	protected List<Agent> getAlive(List<Agent> agentList) {
		return agentList.stream().filter(a -> isAlive(a)).collect(Collectors.toList());
	}
	
	/**
	 * エージェントのリストからランダムに1エージェントを選ぶ
	 *
	 * @param agentList エージェントのリスト
	 * @return リストが空の場合null
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
		// 前のゲームを引きずらないようにフィールドをクリアしておく
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
		
		// 信頼度の初期化
		List<Agent> agent_all = gameInfo.getAgentList();
		for(int i = 0; i < agent_all.size(); i++) {
			trust.put(agent_all.get(i), 0.5); // 全てのエージェントに対して0.5で初期化
		}
		agent_all.clear();
	}

	@Override
	public Agent attack() {
		// 襲撃先が追放された場合
		if(!isAlive(voteTarget)) {
			List<Agent> target = getOthers(gameInfo.getAliveAgentList());
			voteTarget = randomSelect(target);
		}
		
		return voteTarget; // 襲撃
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
		
		// 信頼度によって人か人狼か決める
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
			// 霊媒結果を待ち行列に入れる
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
		this.gameInfo = gameInfo; // ゲーム状況更新
		for (int i = talkListHead; i < gameInfo.getTalkList().size(); i++) { // 未解析会話の解析
			Talk talk = gameInfo.getTalkList().get(i); // 解析対象会話
			Agent talker = talk.getAgent(); // 発言したエージェント
			if (talker == me) {// 自分の発言は解析しない
				continue;
			}
			// 会話文字列そのものよりもContentを作成した方が処理しやすい
			Content content = new Content(talk.getText());
			switch (content.getTopic()) {
			case VOTE:
				num_voteCandidate.put(talker, content.getTarget()); // 自分以外の投票先を把握
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
	talkListHead = gameInfo.getTalkList().size(); // すべて解析済みとする
	}
	
	@Override
	public String talk() {
		// 会話をしながら投票先と襲撃先を決めていく
		List<Agent> target = getOthers(gameInfo.getAliveAgentList()); // 襲撃先候補
		List<Agent> candidates = getOthers(gameInfo.getAliveAgentList()); // 投票先候補
		
		// 予定日以降に他の霊媒師が一人以下のとき確率的にCO
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
		
		// CO後は霊能行使結果を報告
		if (hasCO && !myJudgeQueue.isEmpty()) {
			Judge judge = myJudgeQueue.poll();
			return new Content(new IdentContentBuilder(me, judge.getTarget(), judge.getResult())).getText();
		}
		
		// 自分（村人）を人狼と判定した占い師のリスト
		List<Agent> RealSeers = divinationReports.stream()
				.filter(j -> j.getResult() == Species.WEREWOLF && j.getTarget() == me).map(j -> j.getAgent()).distinct()
				.collect(Collectors.toList());
		for(int i = 0; i < RealSeers.size(); i++) {
			trust.replace(RealSeers.get(i), 0.9); // 自分を人狼判定した人はほぼ確実に村人陣営
		}
		// 自分（村人）を村人と判定した占い師のリスト
		List<Agent> FeelSeers = divinationReports.stream()
				.filter(j -> j.getResult() == Species.HUMAN && j.getTarget() == me).map(j -> j.getAgent()).distinct()
				.collect(Collectors.toList());
		for(int i = 0; i < FeelSeers.size(); i++) {
			trust.replace(FeelSeers.get(i), 0.1); // 自分を人狼判定した人はほぼ確実に人狼陣営
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
		
		// 生存本物占い師に投票
		if (candidates.isEmpty()) {
			candidates = getAlive(RealSeers);
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
		
		// 信頼度の高い生存エージェントに投票または襲撃
		if (candidates.isEmpty()) {
			// 他の信頼度の高い人物を襲撃
			List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
			List<Agent> trust_agent = new ArrayList<Agent>();  // 信頼度が最も高い人物
			double most_trust = 0.5; // 信頼度が最も高い人物の信頼度
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
			// 占い師が一人の場合は自分を村人と言っていない限り襲撃
			List<Agent> aliveSeer = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.SEER)
					.collect(Collectors.toList());
			if(aliveSeer.size() == 1) {
				target.add(aliveSeer.get(0));
			}
			
			// 占い師以外で一人しかカミングアウトしていない役職の人物は信頼度が上がる
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
			// 他の信頼度の高い人物を襲撃
			List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
			List<Agent> trust_agent = new ArrayList<Agent>();  // 信頼度が最も高い人物
			double most_trust = 0.5; // 信頼度が最も高い人物の信頼度
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
				
		// それでもいなければ生存エージェントに投票または襲撃
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
	
		// 投票先と襲撃先
		if (voteCandidate == null || !target.contains(voteCandidate)) {
			voteTarget = randomSelect(target);
		}
		// 初めての投票先宣言あるいは変更ありの場合，投票先宣言
		if (voteCandidate == null || !candidates.contains(voteCandidate)) {
			voteCandidate = randomSelect(candidates);
			return new Content(new VoteContentBuilder(me, voteCandidate)).getText();
		}
		
		return Talk.SKIP; // 様子見
	}
	
	@Override
	public Agent vote() {
		return voteCandidate;
	}

	@Override
	public String whisper() {
		// 囁きを通して最終的な襲撃先を決めていく
		if(whisper_index == 0) {
			whisper_index++;
			return new Content(new ComingoutContentBuilder(me, Role.WEREWOLF)).getText();
		}
		
		// 他の人狼がカミングアウトしたとき
		OtherWerewolf = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.WEREWOLF)
				.collect(Collectors.toList());
		
		// 襲撃先が被った場合
		if(Other_voteTarget.size() > 0) {
			for(int i = 0; i < Other_voteTarget.size(); i++) {
				if(voteTarget == Other_voteTarget.get(i)) {
					// 他の信頼度の高い人物を襲撃
					List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
					List<Agent> trust_agent = new ArrayList<Agent>();  // 信頼度が最も高い人物
					double most_trust = 0.5; // 信頼度が最も高い人物の信頼度
					for(int j = 0; j < agent_alive.size(); j++) {
						double now_trust = trust.get(agent_alive.get(j));
						if(trust_agent.isEmpty() || now_trust >= most_trust) {
							if(now_trust > most_trust) { trust_agent.clear(); }
							trust_agent.add(agent_alive.get(j));
							most_trust = now_trust;
						}
					}
					voteTarget = randomSelect(trust_agent);
					
					// いなければランダムに襲撃
					if (trust_agent.isEmpty()) {
						voteTarget = randomSelect(agent_alive);
					}
					agent_alive.clear();
					//return new Content(new AttackContentBuilder(me, voteTarget)).getText();
				}
			}
		}
		
		return Talk.SKIP; // 様子見
	}
	
	@Override
	public Agent divine() {
	throw new UnsupportedOperationException(); // 誤使用の場合例外送出
	}
	
	@Override
	public Agent guard() {
	throw new UnsupportedOperationException(); // 誤使用の場合例外送出
	}
	
	@Override
	public void finish() {
	}

}
