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

import dsa41basis.fight.Attack;
import dsa41basis.fight.WithAttack;
import dsa41basis.fight.WithDefense;
import dsa41basis.hero.DerivedValue;
import dsa41basis.hero.Energy;
import dsatool.util.Tuple;
import jsonant.value.JSONObject;

public class NPCParticipant extends Participant {

	public NPCParticipant(final JSONObject npc) {
		this(npc, ParticipantType.NPC);
	}

	protected NPCParticipant(final JSONObject npc, final ParticipantType type) {
		super(npc,
				npc.getObj("Biografie").getStringOrDefault("Name", "Unbenannt"),
				new DerivedValue("Initiative-Basis", new JSONObject(null), npc),
				new DerivedValue("Initiative", new JSONObject(null), npc),
				new Energy("Lebensenergie", new JSONObject(null), npc),
				new Energy("Ausdauer", new JSONObject(null), npc),
				npc.getObj("Basiswerte").containsKey("Astralenergie") ? new Energy("Astralenergie", new JSONObject(null), npc) : null,
				npc.getObj("Basiswerte").containsKey("Karmaenergie") ? new Energy("Karmaenergie", new JSONObject(null), npc) : null,
				type);
	}

	@Override
	protected Tuple<List<WithAttack>, List<WithDefense>> getAttacksAndDefenses() {
		final List<WithAttack> attacks = new ArrayList<>();
		final List<WithDefense> defenses = new ArrayList<>();

		final JSONObject actualAttacks = getParticipant().getObj("Angriffe");
		for (final String attackName : actualAttacks.keySet()) {
			final Attack attack = new Attack(attackName, actualAttacks.getObj(attackName));
			attacks.add(attack);
			defenses.add(attack);
		}

		return new Tuple<>(attacks, defenses);
	}
}
