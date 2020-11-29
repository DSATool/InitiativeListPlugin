/*
 * Copyright 2017 DSATool team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package initiativelist.participants;

import java.util.List;

import dsa41basis.fight.WithAttack;
import dsa41basis.fight.WithDefense;
import dsa41basis.hero.DerivedValue;
import dsa41basis.hero.Energy;
import dsa41basis.util.DSAUtil;
import dsatool.util.Tuple;
import jsonant.value.JSONObject;

public abstract class Participant {

	public enum ParticipantType {
		Hero, NPC, Animal
	}

	private final JSONObject participant;

	private final String name;
	private final DerivedValue iniBase;
	private final DerivedValue ini;
	private final Energy lep;
	private final Energy aup;
	private final Energy asp;
	private final Energy kap;
	private final List<WithAttack> attacks;
	private final List<WithDefense> defenses;

	private final ParticipantType type;

	public Participant(final JSONObject participant, final String name, final DerivedValue iniBase, final DerivedValue ini, final Energy lep, final Energy aup,
			final Energy asp, final Energy kap, final ParticipantType type) {
		this.participant = participant;
		this.name = name;
		this.iniBase = iniBase;
		this.ini = ini;
		this.lep = lep;
		this.aup = aup;
		this.asp = asp;
		this.kap = kap;
		this.type = type;

		final Tuple<List<WithAttack>, List<WithDefense>> attacksAndDefenses = getAttacksAndDefenses();
		attacks = attacksAndDefenses._1;
		defenses = attacksAndDefenses._2;
	}

	public Energy getAsp() {
		return asp;
	}

	public List<WithAttack> getAttacks() {
		return attacks;
	}

	protected abstract Tuple<List<WithAttack>, List<WithDefense>> getAttacksAndDefenses();

	public Energy getAup() {
		return aup;
	}

	public List<WithDefense> getDefenses() {
		return defenses;
	}

	public DerivedValue getIni() {
		return ini;
	}

	public DerivedValue getIniBase() {
		return iniBase;
	}

	public Energy getKap() {
		return kap;
	}

	public Energy getLep() {
		return lep;
	}

	public String getName() {
		return name;
	}

	public JSONObject getParticipant() {
		return participant;
	}

	public ParticipantType getType() {
		return type;
	}

	public void randomizeIni(final boolean npcsOnly) {
		int iniRoll = 0;
		for (int i = 0; i < getIni().getActual().getIntOrDefault("Würfel:Anzahl",
				participant.getObj("Sonderfertigkeiten").containsKey("Klingentänzer") ? 2 : 1); ++i) {
			iniRoll += DSAUtil.diceRoll(getIni().getActual().getIntOrDefault("Würfel:Typ", 6));
		}
		getIni().setManualModifier(iniRoll);
	}
}
