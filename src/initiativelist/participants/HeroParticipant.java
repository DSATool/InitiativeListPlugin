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

import java.util.ArrayList;
import java.util.List;

import dsa41basis.fight.CloseCombatWeapon;
import dsa41basis.fight.DefensiveWeapon;
import dsa41basis.fight.RangedWeapon;
import dsa41basis.fight.WithAttack;
import dsa41basis.fight.WithDefense;
import dsa41basis.hero.DerivedValue;
import dsa41basis.hero.Energy;
import dsa41basis.util.DSAUtil;
import dsa41basis.util.HeroUtil;
import dsatool.resources.ResourceManager;
import dsatool.util.Tuple;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class HeroParticipant extends Participant {

	public HeroParticipant(final JSONObject hero) {
		super(hero,
				hero.getObj("Biografie").getStringOrDefault("Vorname", "Unbenannt"),
				new DerivedValue("Initiative-Basis", ResourceManager.getResource("data/Basiswerte").getObj("Initiative-Basis"), hero),
				new DerivedValue("Initiative", ResourceManager.getResource("data/Basiswerte").getObj("Initiative"), hero),
				new Energy("Lebensenergie", ResourceManager.getResource("data/Basiswerte").getObj("Lebensenergie"), hero),
				new Energy("Ausdauer", ResourceManager.getResource("data/Basiswerte").getObj("Ausdauer"), hero),
				HeroUtil.isMagical(hero) ? new Energy("Astralenergie", ResourceManager.getResource("data/Basiswerte").getObj("Astralenergie"), hero) : null,
				HeroUtil.isClerical(hero, false) ? new Energy("Karmaenergie", new JSONObject(null), hero) : null,
				ParticipantType.Hero);
	}

	@Override
	protected Tuple<List<WithAttack>, List<WithDefense>> getAttacksAndDefenses() {
		final List<WithAttack> attacks = new ArrayList<>();
		final List<WithDefense> defenses = new ArrayList<>();

		final JSONObject talents = ResourceManager.getResource("data/Talente");
		final JSONObject closeCombatTalents = talents.getObj("Nahkampftalente");
		final JSONObject actualCloseCombatTalents = getParticipant().getObj("Talente").getObj("Nahkampftalente");
		final JSONObject rangedCombatTalents = talents.getObj("Fernkampftalente");
		final JSONObject actualRangedCombatTalents = getParticipant().getObj("Talente").getObj("Fernkampftalente");
		final JSONArray items = getParticipant().getObj("Besitz").getArr("Ausrüstung");

		attacks.add(new CloseCombatWeapon(getParticipant(), HeroUtil.infight, HeroUtil.infight, closeCombatTalents, actualCloseCombatTalents));

		DSAUtil.foreach(item -> item.containsKey("Kategorien"), item -> {
			final JSONArray categories = item.getArr("Kategorien");

			if (categories.contains("Nahkampfwaffe")) {
				final CloseCombatWeapon weapon = new CloseCombatWeapon(getParticipant(),
						item.containsKey("Nahkampfwaffe") ? item.getObj("Nahkampfwaffe") : item, item, closeCombatTalents, actualCloseCombatTalents);
				attacks.add(weapon);
				if (weapon.getPa() != Integer.MIN_VALUE) {
					defenses.add(weapon);
				}
			}
			if (categories.contains("Fernkampfwaffe")) {
				attacks.add(new RangedWeapon(getParticipant(), item.containsKey("Fernkampfwaffe") ? item.getObj("Fernkampfwaffe") : item, item,
						rangedCombatTalents, actualRangedCombatTalents));
			}
			if (categories.contains("Schild")) {
				defenses.add(new DefensiveWeapon(true, getParticipant(), item.containsKey("Schild") ? item.getObj("Schild") : item, item));
			}
		}, items);

		return new Tuple<>(attacks, defenses);
	}

	@Override
	public void randomizeIni(final boolean npcsOnly) {
		if (!npcsOnly) {
			super.randomizeIni(npcsOnly);
		}
	}
}
