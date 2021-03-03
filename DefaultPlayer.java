/**
 * DefaultPlayer.java
 */
package jp.ac.yamagata_u.st.momma.aiwolf;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * デフォルトプレイヤー
 * 
 * @author mon25
 *
 */
public class DefaultPlayer implements Player {

	@Override
	public Agent attack() {
		return null; // 襲撃先はお任せ
	}

	@Override
	public void dayStart() {
	}

	@Override
	public Agent divine() {
		return null; // 占わない
	}

	@Override
	public void finish() {
	}

	@Override
	public String getName() {
		return "Momma";
	}

	@Override
	public Agent guard() {
		return null; // 誰も護衛しない
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
	}

	@Override
	public String talk() {
		return Talk.SKIP; // 様子見
	}

	@Override
	public void update(GameInfo gameInfo) {
	}

	@Override
	public Agent vote() {
		return null; // ランダム投票
	}

	@Override
	public String whisper() {
		return Talk.SKIP; // 様子見
	}

}
