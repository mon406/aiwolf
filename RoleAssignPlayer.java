/**
 * RoleAssignPlayer.java
 */
package jp.ac.yamagata_u.st.momma.aiwolf;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 割り当てられた役職に応じてクラスを使い分けるプレイヤークラス
 * 
 * @author mon25
 *
 */
public class RoleAssignPlayer implements Player {

	private Player defaultPlayer = new DefaultPlayer();
	private Player villager = new Villager();
	private Player bodyguard = new Bodyguard();
	private Player medium = new Medium();
	private Player seer = new Seer();
	private Player possessed = new Possessed();
	private Player werewolf = new Werewolf();
	
	private Player player = defaultPlayer;
	
	@Override
	public Agent attack() {
		return player.attack();
	}

	@Override
	public void dayStart() {
		player.dayStart();
	}

	@Override
	public Agent divine() {
		return player.divine();
	}

	@Override
	public void finish() {
		player.finish();
	}

	@Override
	public String getName() {
		return "Momma";
	}

	@Override
	public Agent guard() {
		return player.guard();
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		switch (gameInfo.getRole()) {
		case VILLAGER:
			player = villager;
			break;
		case BODYGUARD:
			player = bodyguard;
			break;
		case MEDIUM:
			player = medium;
			break;
		case SEER:
			player = seer;
			break;
		case POSSESSED:
			player = possessed;
			break;
		case WEREWOLF:
			player = werewolf;
			break;
		default:
			player = defaultPlayer;
			break;
		}
		player.initialize(gameInfo, gameSetting);
	}

	@Override
	public String talk() {
		return player.talk();
	}

	@Override
	public void update(GameInfo gameInfo) {
		player.update(gameInfo);
	}

	@Override
	public Agent vote() {
		return player.vote();
	}

	@Override
	public String whisper() {
		return player.whisper();
	}

}
