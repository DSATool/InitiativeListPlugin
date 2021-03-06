/*
 * Copyright 2020 DSATool team
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
package initiativelist;

import dsatool.gui.Main;
import dsatool.plugins.Plugin;
import initiativelist.ui.InitiativeListController;

/**
 * A plugin for managing an initiative list
 *
 * @author Dominik Helm
 */
public class InitiativeList extends Plugin {

	private InitiativeListController controller;

	/*
	 * (non-Javadoc)
	 *
	 * @see plugins.Plugin#getPluginName()
	 */
	@Override
	public String getPluginName() {
		return "InitiativeList";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see plugins.Plugin#initialize()
	 */
	@Override
	public void initialize() {
		Main.addDetachedToolComposite("Kampf", "Initiative", 736, 550, () -> {
			controller = new InitiativeListController();
			return controller.getRoot();
		}, window -> {
			controller.setStage(window);
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see plugins.Plugin#load()
	 */
	@Override
	protected void load() {}
}
