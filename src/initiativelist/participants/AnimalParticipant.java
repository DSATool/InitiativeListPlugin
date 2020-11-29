package initiativelist.participants;

import jsonant.value.JSONObject;

public class AnimalParticipant extends NPCParticipant {

	public AnimalParticipant(final JSONObject animal) {
		super(animal, ParticipantType.Animal);
	}

	@Override
	public void randomizeIni(final boolean npcsOnly) {
		if (!npcsOnly) {
			super.randomizeIni(npcsOnly);
		}
	}
}
