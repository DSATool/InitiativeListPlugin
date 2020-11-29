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
package initiativelist.ui;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import dsatool.resources.ResourceManager;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import initiativelist.participants.AnimalParticipant;
import initiativelist.participants.HeroParticipant;
import initiativelist.participants.NPCParticipant;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class InitiativeListController {
	private static final Comparator<ParticipantUI> participantSorting = (left, right) -> {
		if (left.isDisabled() != right.isDisabled())
			return left.isDisabled() ? 1 : -1;
		else {
			final int iniDiff = right.getIni() - left.getIni();
			return iniDiff == 0 ? right.getBaseIni() - left.getBaseIni() : iniDiff;
		}
	};
	private static final Collator comparator = Collator.getInstance(Locale.GERMANY);
	@FXML
	private Region root;
	@FXML
	private Button menuButton;
	@FXML
	private ReactiveSpinner<Integer> round;
	@FXML
	private ReactiveSpinner<Integer> phase;
	@FXML
	private ListView<ParticipantUI> list;
	@FXML
	private TreeView<JSONObject> heroesList;
	@FXML
	private TreeItem<JSONObject> heroesRoot;

	@FXML
	private TreeView<Tuple<String, JSONObject>> npcsList;

	@FXML
	private TreeItem<Tuple<String, JSONObject>> npcsRoot;
	private Stage window;

	private final ObservableList<ParticipantUI> participants;
	private final Map<JSONObject, Object> activeParticipants = new IdentityHashMap<>();

	private final ObjectProperty<ParticipantUI> selected = new SimpleObjectProperty<>(null);
	private final MultipleSelectionModel<ParticipantUI> selection;

	public InitiativeListController() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("InitiativeListController.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		root.getStylesheets().add(getClass().getResource("initiativelist.css").toExternalForm());

		round.getStyleClass().clear();
		phase.getStyleClass().clear();

		list.setCellFactory(list -> new ListCell<>() {
			@Override
			public void updateItem(final ParticipantUI item, final boolean isEmpty) {
				super.updateItem(item, isEmpty);
				if (isEmpty) {
					setGraphic(null);
				} else {
					setGraphic(item.getRoot());
				}
			}
		});

		participants = FXCollections.observableArrayList(item -> new Observable[] { item.getRoot().heightProperty() });
		list.setItems(participants.sorted(participantSorting));

		root.sceneProperty().addListener((o, oldV, newV) -> {
			if (newV != null) {
				final DoubleBinding height = newV.heightProperty().subtract(34);
				list.maxHeightProperty().bind(height);
				list.prefHeightProperty().bind(height);
				list.maxHeightProperty().bind(height);
			}
		});

		selection = list.getSelectionModel();

		selected.addListener((o, oldV, newV) -> {
			selection.select(newV);
			list.scrollTo(newV);
			if (newV != null) {
				phase.getValueFactory().setValue(newV.getIni());
			}
		});
		selection.getSelectedItems().addListener((final Change<? extends ParticipantUI> c) -> {
			final ObservableList<? extends ParticipantUI> list = c.getList();
			if (list.size() > 0 && list.get(0) != selected.get() && selected.get() != null || list.size() > 1) {
				selection.select(selected.get());
			}
		});

		heroesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		final StringConverter<JSONObject> heroConverter = new StringConverter<>() {
			@Override
			public JSONObject fromString(final String arg0) {
				return null;
			}

			@Override
			public String toString(final JSONObject item) {
				return getHeroName(item);
			}
		};

		heroesList.setCellFactory(tv -> {
			final TreeCell<JSONObject> cell = new TextFieldTreeCell<>(heroConverter);

			final ContextMenu menu = new ContextMenu();
			final MenuItem addItem = new MenuItem("Hinzufügen");
			addItem.setOnAction(e -> {
				@SuppressWarnings("unchecked")
				final TreeItem<JSONObject>[] selectedItems = heroesList.getSelectionModel().getSelectedItems().toArray(new TreeItem[0]);
				for (final TreeItem<JSONObject> selected : selectedItems) {
					final JSONObject hero = selected.getValue();
					if (!activeParticipants.containsKey(hero)) {
						participants.add(new ParticipantUI(hero.getParent() == null ? new HeroParticipant(hero) : new AnimalParticipant(hero), this));
					}
					activeParticipants.put(hero, null);
					if (selected.getChildren().isEmpty()) {
						final TreeItem<JSONObject> parent = selected.getParent();
						final ObservableList<TreeItem<JSONObject>> siblings = parent.getChildren();
						siblings.remove(selected);
						if (siblings.isEmpty() && activeParticipants.containsKey(parent.getValue())) {
							parent.getParent().getChildren().remove(parent);
						}
					}
				}
			});
			menu.getItems().add(addItem);
			cell.contextMenuProperty().bind(Bindings.when(cell.itemProperty().isNotNull()).then(menu).otherwise((ContextMenu) null));

			return cell;
		});

		npcsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		final StringConverter<Tuple<String, JSONObject>> npcConverter = new StringConverter<>() {
			@Override
			public Tuple<String, JSONObject> fromString(final String newName) {
				return new Tuple<>(newName, null);
			}

			@Override
			public String toString(final Tuple<String, JSONObject> item) {
				return getNPCName(item);
			}
		};

		npcsList.setCellFactory(tv -> {
			final TreeCell<Tuple<String, JSONObject>> cell = new TextFieldTreeCell<>(npcConverter);

			final ContextMenu menu = new ContextMenu();
			final MenuItem addItem = new MenuItem("Hinzufügen");
			addItem.setOnAction(e -> {
				final List<TreeItem<Tuple<String, JSONObject>>> toRemove = new ArrayList<>();
				npcsList.getSelectionModel().getSelectedItems().forEach(item -> activateNPCs(item, toRemove));
				toRemove.forEach(item -> {
					final TreeItem<Tuple<String, JSONObject>> parent = item.getParent();
					if (parent != null) {
						parent.getChildren().remove(item);
					}
				});
			});
			menu.getItems().add(addItem);
			cell.contextMenuProperty().bind(Bindings.when(cell.itemProperty().isNotNull()).then(menu).otherwise((ContextMenu) null));

			return cell;
		});

		fillHeroList();
		fillNPCList();

		reset();
	}

	private void activateNPCs(final TreeItem<Tuple<String, JSONObject>> item, final List<TreeItem<Tuple<String, JSONObject>>> toRemove) {
		final JSONObject npc = item.getValue()._2;
		if (npc == null) {
			item.getChildren().forEach(child -> activateNPCs(child, toRemove));
			toRemove.add(item);
		} else if (!activeParticipants.containsKey(npc)) {
			participants.add(new ParticipantUI(new NPCParticipant(npc), this));
			activeParticipants.put(npc, null);
			toRemove.add(item);
		}
	}

	private void addNPC(final JSONObject npc) {
		final JSONArray groups = npc.getObj("Biografie").getArr("Gruppe");
		TreeItem<Tuple<String, JSONObject>> current = npcsRoot;
		for (final String groupName : groups.getStrings()) {
			final TreeItem<Tuple<String, JSONObject>> group = new TreeItem<>(new Tuple<>(groupName, null));
			final ObservableList<TreeItem<Tuple<String, JSONObject>>> children = current.getChildren();
			final Optional<TreeItem<Tuple<String, JSONObject>>> groupItem = children.stream().filter(item -> groupName.equals(item.getValue()._1))
					.findFirst();
			if (groupItem.isPresent()) {
				current = groupItem.get();
			} else {
				current = group;
				children.add(group);
			}
		}
		final TreeItem<Tuple<String, JSONObject>> item = new TreeItem<>(new Tuple<>(null, npc));
		current.getChildren().add(item);
		current.getChildren().sort((left, right) -> comparator.compare(getNPCName(left.getValue()), getNPCName(right.getValue())));
	}

	@FXML
	private void advancePhase() {
		setState(false);
		int index = selection.getSelectedIndex() + 1;
		if (index >= list.getItems().size() || list.getItems().get(index).isDisabled()) {
			index = 0;
			round.getValueFactory().setValue(round.getValue() + 1);
		}
		if (index < list.getItems().size()) {
			selected.set(list.getItems().get(index));
		}
	}

	@FXML
	private void advanceRound() {
		setState(false);
		round.getValueFactory().setValue(round.getValue() + 1);
		selected.set(list.getItems().get(0));
	}

	private void fillHeroList() {
		heroesRoot.getChildren().clear();
		final List<JSONObject> heroes = ResourceManager.getAllResources("characters/");
		for (final JSONObject hero : heroes) {
			final TreeItem<JSONObject> heroItem = new TreeItem<>(hero);
			final ObservableList<TreeItem<JSONObject>> children = heroItem.getChildren();
			if (hero.containsKey("Tiere")) {
				for (final JSONObject animal : hero.getArr("Tiere").getObjs()) {
					if (!activeParticipants.containsKey(animal)) {
						children.add(new TreeItem<>(animal));
					}
				}
			}
			if (!children.isEmpty() || !activeParticipants.containsKey(hero)) {
				heroesRoot.getChildren().add(heroItem);
			}
		}
	}

	private void fillNPCList() {
		npcsRoot.getChildren().clear();
		final List<JSONObject> npcs = ResourceManager.getAllResources("npcs/");
		for (final JSONObject npc : npcs) {
			addNPC(npc);
		}
	}

	private String getHeroName(final JSONObject hero) {
		return hero != null ? hero.getParent() == null ? hero.getObj("Biografie").getStringOrDefault("Vorname", "Unbenannt")
				: hero.getObj("Biografie").getStringOrDefault("Name", "Unbenannt") : "";
	}

	private String getNPCName(final Tuple<String, JSONObject> item) {
		final JSONObject npc = item._2;
		if (npc != null)
			return npc.getObj("Biografie").getStringOrDefault("Name", "Unbenannt");
		else
			return item._1;
	}

	public Parent getRoot() {
		return root;
	}

	public void removeActiveParticipants() {
		final ParticipantUI[] selectedItems = list.getSelectionModel().getSelectedItems().toArray(new ParticipantUI[0]);
		for (final ParticipantUI removed : selectedItems) {
			participants.remove(removed);
			final JSONObject participant = removed.getParticipant().getParticipant();
			activeParticipants.remove(participant);
			switch (removed.getParticipant().getType()) {
				case Hero -> {
					final Optional<TreeItem<JSONObject>> optionalItem = heroesRoot.getChildren().stream().filter(item -> item.getValue() == participant)
							.findFirst();
					if (optionalItem.isEmpty()) {
						heroesRoot.getChildren().add(new TreeItem<>(participant));
						heroesRoot.getChildren().sort((left, right) -> comparator.compare(getHeroName(left.getValue()), getHeroName(right.getValue())));
					}
				}

				case NPC -> addNPC(participant);

				case Animal -> {
					final JSONObject hero = (JSONObject) participant.getParent().getParent();
					final Optional<TreeItem<JSONObject>> optionalItem = heroesRoot.getChildren().stream().filter(item -> item.getValue() == hero).findFirst();
					TreeItem<JSONObject> heroItem;
					if (optionalItem.isEmpty()) {
						heroItem = new TreeItem<>(hero);
						heroesRoot.getChildren().add(heroItem);
						heroesRoot.getChildren().sort((left, right) -> comparator.compare(getHeroName(left.getValue()), getHeroName(right.getValue())));
					} else {
						heroItem = optionalItem.get();
					}
					heroItem.getChildren().add(new TreeItem<>(participant));
				}
			}
		}
	}

	public void reset() {
		if (!participants.isEmpty()) {
			selected.set(participants.get(0));
		}

		round.getValueFactory().setValue(1);
	}

	public void resortParticipants() {
		final SortedList<ParticipantUI> sortedList = (SortedList<ParticipantUI>) list.getItems();
		sortedList.setComparator(null);
		sortedList.setComparator(participantSorting);
	}

	public void setStage(final Stage window) {
		this.window = window;
		setState(participants.isEmpty());
	}

	private void setState(final boolean selectParticipants) {
		if (selectParticipants != heroesList.isManaged()) {
			window.setX(window.getX() + (selectParticipants ? -248 : 248));
			if (!selectParticipants) {
				selection.clearSelection();
			}
			participants.forEach(p -> p.setPreparationStage(selectParticipants));
		}
		if (selectParticipants) {
			selected.set(null);
		}
		list.getSelectionModel().setSelectionMode(selectParticipants ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
		heroesList.setVisible(selectParticipants);
		heroesList.setManaged(selectParticipants);
		npcsList.setVisible(selectParticipants);
		npcsList.setManaged(selectParticipants);
		window.setMinWidth(selectParticipants ? 736 : 240);
		window.setMaxWidth(selectParticipants ? 736 : 240);
	}

	@FXML
	private void showMenu() {
		final ContextMenu menu = new ContextMenu();

		final MenuItem participantsItem = new MenuItem("Kampfbeteiligte");
		participantsItem.setOnAction(event -> setState(!heroesList.isManaged()));

		final MenuItem randomIniItem = new MenuItem("Zufällige Initiative");
		randomIniItem.setOnAction(event -> participants.forEach(p -> p.getParticipant().randomizeIni(false)));

		final MenuItem randomNPCIniItem = new MenuItem("Zufällige Initiative (NSCs)");
		randomNPCIniItem.setOnAction(event -> participants.forEach(p -> p.getParticipant().randomizeIni(true)));

		final MenuItem resetItem = new MenuItem("Kampfbeginn");
		resetItem.setOnAction(event -> {
			setState(false);
			reset();
		});

		menu.getItems().addAll(participantsItem, randomIniItem, randomNPCIniItem, resetItem);
		menu.show(menuButton, Side.BOTTOM, 0, 0);
	}
}
