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

import dsa41basis.fight.WithAttack;
import dsa41basis.fight.WithDefense;
import dsa41basis.hero.DerivedValue;
import dsa41basis.hero.Energy;
import dsa41basis.ui.hero.SingleRollDialog;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import initiativelist.participants.Participant;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import jsonant.value.JSONObject;

public class ParticipantUI {
	@FXML
	private VBox root;
	@FXML
	private Label nameLabel;
	@FXML
	private ReactiveSpinner<Integer> iniSpinner;
	@FXML
	private ProgressBar lepBar;
	@FXML
	private ProgressBar aupBar;
	@FXML
	private ProgressBar aspBar;
	@FXML
	private ProgressBar kapBar;

	private final Participant participant;
	private final InitiativeListController controller;

	private final BooleanProperty disabled = new SimpleBooleanProperty(false);

	private final BooleanProperty preparationStage = new SimpleBooleanProperty(true);

	public ParticipantUI(final Participant participant, final InitiativeListController controller) {
		this.participant = participant;
		this.controller = controller;

		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("ParticipantUI.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		nameLabel.setText(participant.getName());

		bindIni();

		bindLeP();

		bindValues(aupBar, participant.getAup(), Energy.COLOR_AUP);

		if (participant.getAsp() != null) {
			aspBar.setManaged(true);
			bindValues(aspBar, participant.getAsp(), Energy.COLOR_ASP);
		}
		if (participant.getKap() != null) {
			kapBar.setManaged(true);
			bindValues(kapBar, participant.getKap(), Energy.COLOR_KAP);
		}

		final ContextMenu menu = new ContextMenu();

		final MenuItem removeItem = new MenuItem("Entfernen");
		menu.getItems().add(removeItem);
		removeItem.visibleProperty().bind(preparationStage);
		removeItem.setOnAction(e -> controller.removeActiveParticipants());

		MenuItem atItem;

		final int numAttacks = participant.getAttacks().size();
		switch (numAttacks) {
			case 0:
				break;

			case 1:
				atItem = new MenuItem("Attacke");
				menu.getItems().add(atItem);
				atItem.visibleProperty().bind(preparationStage.not());
				atItem.setOnAction(e -> new SingleRollDialog(root.getScene().getWindow(), SingleRollDialog.Type.ATTACK, participant.getParticipant(),
						participant.getAttacks().get(0)));
				break;

			default:
				atItem = new Menu("Attacke");
				menu.getItems().add(atItem);
				atItem.visibleProperty().bind(preparationStage.not());

				for (final WithAttack attack : participant.getAttacks()) {
					final MenuItem weaponItem = new MenuItem(attack.getName());
					weaponItem.setOnAction(
							e -> new SingleRollDialog(root.getScene().getWindow(), SingleRollDialog.Type.ATTACK, participant.getParticipant(), attack));
					((Menu) atItem).getItems().add(weaponItem);
				}

				break;
		}

		MenuItem paItem;

		final int numDefenses = participant.getDefenses().size();
		switch (numDefenses) {
			case 0:
				break;

			case 1:
				paItem = new MenuItem("Parade");
				menu.getItems().add(paItem);
				paItem.visibleProperty().bind(preparationStage.not());
				paItem.setOnAction(e -> new SingleRollDialog(root.getScene().getWindow(), SingleRollDialog.Type.DEFENSE, participant.getParticipant(),
						participant.getDefenses().get(0)));
				break;

			default:
				paItem = new Menu("Parade");
				menu.getItems().add(paItem);
				paItem.visibleProperty().bind(preparationStage.not());

				for (final WithDefense defense : participant.getDefenses()) {
					final MenuItem weaponItem = new MenuItem(defense.getName());
					weaponItem.setOnAction(
							e -> new SingleRollDialog(root.getScene().getWindow(), SingleRollDialog.Type.DEFENSE, participant.getParticipant(), defense));
					((Menu) paItem).getItems().add(weaponItem);
				}

				break;
		}

		root.setOnContextMenuRequested(e -> {
			if (!e.isConsumed()) {
				menu.show(root, e.getScreenX(), e.getScreenY());
			}
		});
	}

	private void bindIni() {
		final DerivedValue iniBase = participant.getIniBase();
		final DerivedValue ini = participant.getIni();

		iniSpinner.getValueFactory().setValue(iniBase.getCurrent() + ini.getCurrent());

		final Timeline timer = new Timeline();
		timer.getKeyFrames().setAll(new KeyFrame(Duration.millis(750)));
		timer.setOnFinished(event -> {
			controller.resortParticipants();
		});

		final ChangeListener<Number> iniListener = (o, oldV, newV) -> {
			if (newV.intValue() != oldV.intValue()) {
				iniSpinner.getValueFactory().setValue(iniBase.getCurrent() + ini.getCurrent());
			}
		};

		ini.currentProperty().addListener(iniListener);
		iniBase.currentProperty().addListener(iniListener);
		iniSpinner.getValueFactory().valueProperty().addListener((o, oldV, newV) -> {
			if (!newV.equals(oldV)) {
				ini.setManualModifier(newV - iniBase.getCurrent());
				timer.playFromStart();
			}
		});

		iniSpinner.disabledProperty().addListener((o, oldV, newV) -> {
			if (!newV.equals(oldV)) {
				timer.playFromStart();
			}
		});
	}

	private void bindLeP() {
		final Energy lep = participant.getLep();

		final BooleanBinding isNegative = lep.currentProperty().lessThan(0);
		isNegative.addListener((o, oldV, newV) -> setColor(lepBar, newV ? Color.BLACK : Energy.COLOR_LEP));

		setColor(lepBar, isNegative.get() ? Color.BLACK : Energy.COLOR_LEP);

		lepBar.progressProperty().bind(Bindings.when(isNegative).then(lep.currentPercentageProperty().multiply(-1))
				.otherwise(lep.currentPercentageProperty()));
		registerHandlers(lepBar, lep, isNegative);

		final EventHandler<? super MouseEvent> changeHandler = lepBar.getOnMouseClicked();
		final EventHandler<? super MouseEvent> tooltipHandler = lepBar.getOnMouseMoved();
		lepBar.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY)) {
				changeHandler.handle(event);
				tooltipHandler.handle(event);
			} else {
				lep.setManualModifier(-getProgressValue(lepBar, event, lep.getMax()) - lep.getMax());
				tooltipHandler.handle(event);
			}
		});

		iniSpinner.disableProperty().bind(disabled);

		disabled.set(isHeroDisabled(lep.getCurrent()));

		lep.currentProperty().addListener((o, oldV, newV) -> {
			disabled.set(isHeroDisabled(newV.intValue()));
		});
	}

	private void bindValues(final ProgressBar bar, final Energy energy, final Color color) {
		setColor(bar, color);
		bar.progressProperty().bind(energy.currentPercentageProperty());
		registerHandlers(bar, energy, new SimpleBooleanProperty(false));
	}

	public int getBaseIni() {
		return participant.getIniBase().getCurrent();
	}

	public int getIni() {
		return iniSpinner.getValue();
	}

	public Participant getParticipant() {
		return participant;
	}

	private int getProgressValue(final ProgressBar bar, final MouseEvent event, final int max) {
		return (int) Math.floor(Math.max(Math.min(event.getX() / bar.getWidth(), 0.999), 0) * (max + 1));
	}

	public Region getRoot() {
		return root;
	}

	public boolean isDisabled() {
		return disabled.get();
	}

	private boolean isHeroDisabled(final int lep) {
		if (lep > 5)
			return false;
		else if (lep <= 0)
			return true;
		else {
			final JSONObject pros = participant.getParticipant().getObj("Vorteile");
			return !pros.containsKey("Eisern") && !pros.containsKey("Zäher Hund");
		}
	}

	private void registerHandlers(final ProgressBar bar, final Energy energy, final BooleanExpression isNegative) {
		final EventHandler<MouseEvent> changeHandler = event -> {
			if (event.getButton().equals(MouseButton.PRIMARY)) {
				energy.setManualModifier(getProgressValue(bar, event, energy.getMax()) - energy.getMax());
			}
		};
		bar.setOnMouseClicked(changeHandler);

		final Tooltip tooltip = new Tooltip();
		final EventHandler<MouseEvent> tooltipHandler = event -> {
			tooltip.setText((isNegative.get() ? "-" : "") + getProgressValue(bar, event, energy.getMax()));
			tooltip.show(bar, event.getScreenX() + 10, event.getScreenY() + 7);
		};
		bar.setOnMouseMoved(tooltipHandler);
		bar.setOnMouseExited(event -> tooltip.hide());
		bar.setOnMouseDragged(event -> {
			if (event.isPrimaryButtonDown()) {
				changeHandler.handle(event);
			}
			final double x = event.getX();
			final double y = event.getY();
			if (x > 0 && x < bar.getWidth() && y > 0 && y < bar.getHeight()) {
				tooltipHandler.handle(event);
			}
		});

		final Timeline timer = new Timeline();
		timer.getKeyFrames().setAll(new KeyFrame(Duration.millis(250)));
		timer.setOnFinished(event -> {
			tooltip.setOpacity(1);
		});
		bar.setOnMouseEntered(event -> {
			tooltip.setOpacity(0);
			timer.playFromStart();
		});
		bar.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);
	}

	private void setColor(final ProgressBar bar, final Color color) {
		bar.setStyle("-fx-accent: #" + color.toString().substring(2, 8) + ";");
	}

	public void setPreparationStage(final boolean isPreparing) {
		preparationStage.set(isPreparing);
	}
}
