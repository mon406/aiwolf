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
 * �f�t�H���g�v���C���[
 * 
 * @author mon25
 *
 */
public class DefaultPlayer implements Player {

	@Override
	public Agent attack() {
		return null; // �P����͂��C��
	}

	@Override
	public void dayStart() {
	}

	@Override
	public Agent divine() {
		return null; // ���Ȃ�
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
		return null; // �N����q���Ȃ�
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
	}

	@Override
	public String talk() {
		return Talk.SKIP; // �l�q��
	}

	@Override
	public void update(GameInfo gameInfo) {
	}

	@Override
	public Agent vote() {
		return null; // �����_�����[
	}

	@Override
	public String whisper() {
		return Talk.SKIP; // �l�q��
	}

}
