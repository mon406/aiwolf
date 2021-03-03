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
 * —ì”}tƒG[ƒWƒFƒ“ƒg
 * 
 * @author mon25
 *
 */
public class Medium extends Villager {

	private int coDate = 3; // CO—\’è“ú
	private boolean foundWolf; // l˜T‚ğ”­Œ©‚µ‚½‚©
	private boolean hasCO; // COÏ‚È‚çtrue
	private Deque<Judge> myJudgeQueue = new LinkedList<>(); // —ì”}Œ‹‰Ê‚Ì‘Ò‚¿s—ñ
	
	private List<Agent> OtherMedium = new ArrayList<>(); // ‘¼‚Ì—ì”}t
	
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
		// —ì”}Œ‹‰Ê‚ğ‘Ò‚¿s—ñ‚É“ü‚ê‚é
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
		List<Agent> candidates = new ArrayList<>(); // “Š•[æŒó•â
		
		// —\’è“ú‚ ‚é‚¢‚Í‘¼‚Ì—ì”}t‚ªCO‚ ‚é‚¢‚Íl˜T‚ğ”­Œ©‚µ‚½‚çCO
		List<Agent> otherMedium = comingoutMap.keySet().stream().filter(a -> isAlive(a) && comingoutMap.get(a) == Role.MEDIUM)
				.collect(Collectors.toList());
		OtherMedium = getAlive(otherMedium);
		if (!hasCO && (gameInfo.getDay() == coDate || !OtherMedium.isEmpty() || foundWolf)) {
			hasCO = true;
			return new Content(new ComingoutContentBuilder(me, me, Role.MEDIUM)).getText();
		}
		
		// COŒã‚Í—ì”\sgŒ‹‰Ê‚ğ•ñ
		if (hasCO && !myJudgeQueue.isEmpty()) {
			Judge judge = myJudgeQueue.poll();
			return new Content(new IdentContentBuilder(me, judge.getTarget(), judge.getResult())).getText();
		}
		
		// ‹Uè‚¢t
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
		// ‹U—ì”}t‚É“Š•[
		List<Agent> fakeMedium = comingoutMap.keySet().stream()
				.filter(a -> isAlive(a) && comingoutMap.get(a) == Role.MEDIUM).collect(Collectors.toList());
		if(!fakeMedium.isEmpty()) {
			for(int i = 0; i < fakeMedium.size(); i++) {
				trust.replace(fakeMedium.get(i), 0.1);
			}
		}
		
		// è‚¢t‚ªE‚³‚ê‚½ê‡Ac‚è‚Ìè‚¢t‚ÌM—Š“x‚ª‰º‚ª‚é
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
		// E‚³‚ê‚½è‚¢t‚Ìè‚¢Œ‹‰Ê‚ÌM—Š“x‚ªã‚ª‚é
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
		
		// è‚¢t‚©‚çl˜T‚Æ”»’è‚³‚ê‚½¶‘¶ƒG[ƒWƒFƒ“ƒg‚ÌM—Š“x‚ğ‰º‚°‚é
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
		
		// ‚¢‚È‚¯‚ê‚Î¶‘¶‹U—ì”}t‚É“Š•[
		if (candidates.isEmpty()) {
			candidates = getAlive(fakeMedium);
			
			// ‹U—ì”}t‚ÌM—Š“x‚ğ‰º‚°‚é
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
		
		// ©•ª‚Ì“¾•[”‚ªÅ‘½‚Ìê‡A‘¼‚Ì“¾•[”‚ª‘½‚¢l‚É“Š•[
		List<Agent> aliveList = gameInfo.getAliveAgentList();
		Map<Agent, Integer> voteList = new HashMap<>();
		for(int i = 0; i < aliveList.size(); i++) { // 0‚Å‰Šú‰»
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
		for(int i = 0; i < aliveList.size(); i++) { // Å‘½“¾•[æ‚ğæ“¾
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
						
		// ‚¢‚È‚¯‚ê‚ÎM—Š“x‚Ì’á‚¢¶‘¶ƒG[ƒWƒFƒ“ƒg‚É“Š•[
		if (candidates.isEmpty()) {
			List<Agent> agent_alive = getOthers(gameInfo.getAliveAgentList());
			List<Agent> untrust_agent = new ArrayList<Agent>();  // M—Š“x‚ªÅ‚à’á‚¢l•¨
			double most_untrust = 0.5; // M—Š“x‚ªÅ‚à’á‚¢l•¨‚ÌM—Š“x
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
		
		// ‚»‚ê‚Å‚à‚¢‚È‚¯‚ê‚Î¶‘¶ƒG[ƒWƒFƒ“ƒg‚É“Š•[
		if (candidates.isEmpty()) {
			candidates = getOthers(gameInfo.getAliveAgentList());
		}
		
		// ‰‚ß‚Ä‚Ì“Š•[æéŒ¾‚ ‚é‚¢‚Í•ÏX‚ ‚è‚Ìê‡C“Š•[æéŒ¾
		if (voteCandidate == null || !candidates.contains(voteCandidate)) {
			voteCandidate = randomSelect(candidates);
			return new Content(new VoteContentBuilder(me, voteCandidate)).getText();
		}
		
		return Talk.SKIP;
	}

}
