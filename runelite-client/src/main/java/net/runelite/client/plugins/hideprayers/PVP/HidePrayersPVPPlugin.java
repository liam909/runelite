/*
 * Copyright (c) 2018, https://runelitepl.us
 * Copyright (c) 2019, Kyle <https://github.com/kyleeld>
 * Copyright (c) 2018, Raiever <https://github.com/Raieverr>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.runelite.client.plugins.hideprayers.PVP;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.hideprayers.util.PrayerTabStates;
import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@PluginDescriptor 
	(
		name = "Show/Hide PVP Prayers",
		description = "Hides specific Prayers in the Prayer tab for PVP.",
		enabledByDefault = false,
		type = PluginType.PVP
	)

public class HidePrayersPVPPlugin extends Plugin
{
	private static final int PRAYER_COUNT = Prayer.values().length;

	private static final List<WidgetInfo> PRAYER_WIDGET_INFO_LIST = ImmutableList.of
	(
		WidgetInfo.PRAYER_THICK_SKIN, //0
		WidgetInfo.PRAYER_BURST_OF_STRENGTH, //1
		WidgetInfo.PRAYER_CLARITY_OF_THOUGHT, //2
		WidgetInfo.PRAYER_SHARP_EYE, //3
		WidgetInfo.PRAYER_MYSTIC_WILL, //4
		WidgetInfo.PRAYER_ROCK_SKIN, //5
		WidgetInfo.PRAYER_SUPERHUMAN_STRENGTH, //6
		WidgetInfo.PRAYER_IMPROVED_REFLEXES, //7
		WidgetInfo.PRAYER_RAPID_RESTORE, //8
		WidgetInfo.PRAYER_RAPID_HEAL, //9
		WidgetInfo.PRAYER_PROTECT_ITEM, //10
		WidgetInfo.PRAYER_HAWK_EYE, //11
		WidgetInfo.PRAYER_MYSTIC_LORE, //12
		WidgetInfo.PRAYER_STEEL_SKIN, //13
		WidgetInfo.PRAYER_ULTIMATE_STRENGTH, //14
		WidgetInfo.PRAYER_INCREDIBLE_REFLEXES, //15
		WidgetInfo.PRAYER_PROTECT_FROM_MAGIC, //16
		WidgetInfo.PRAYER_PROTECT_FROM_MISSILES, //17
		WidgetInfo.PRAYER_PROTECT_FROM_MELEE, //18
		WidgetInfo.PRAYER_EAGLE_EYE, //19
		WidgetInfo.PRAYER_MYSTIC_MIGHT, //20
		WidgetInfo.PRAYER_RETRIBUTION, //21
		WidgetInfo.PRAYER_REDEMPTION, //22
		WidgetInfo.PRAYER_SMITE, //23
		WidgetInfo.PRAYER_PRESERVE, //24
		WidgetInfo.PRAYER_CHIVALRY, //25
		WidgetInfo.PRAYER_PIETY,  //26
		WidgetInfo.PRAYER_RIGOUR, //27
		WidgetInfo.PRAYER_AUGURY //28
	);

	@Inject
	private Client client;

	@Inject
	private HidePrayersPVPConfig config;

	@Provides
	HidePrayersPVPConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HidePrayersPVPConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		hidePrayers();
	}

	@Override
	protected void shutDown() throws Exception
	{
		restorePrayers();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			hidePrayers();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("hideprayersPVP"))
		{
			hidePrayers();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == WidgetID.PRAYER_GROUP_ID || event.getGroupId() == WidgetID.QUICK_PRAYERS_GROUP_ID)
		{
			hidePrayers();
		}
	}

	private PrayerTabStates getPrayerTabState()
	{
		HashTable<WidgetNode> componentTable = client.getComponentTable();
		for (WidgetNode widgetNode : componentTable.getNodes())
		{
			if (widgetNode.getId() == WidgetID.PRAYER_GROUP_ID)
			{
				return PrayerTabStates.PRAYERS;
			} else if (widgetNode.getId() == WidgetID.QUICK_PRAYERS_GROUP_ID)
			{
				return PrayerTabStates.QUICK_PRAYERS;
			}
		}
		return PrayerTabStates.NONE;
	}

	private void restorePrayers()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		PrayerTabStates prayerTabState = getPrayerTabState();

		if (prayerTabState == PrayerTabStates.PRAYERS) {
			List<Widget> prayerWidgets = PRAYER_WIDGET_INFO_LIST.stream().map(client::getWidget)
					.filter(Objects::nonNull).collect(Collectors.toList());

			if (prayerWidgets.size() != PRAYER_WIDGET_INFO_LIST.size())
				return;

			for (int index = 0; index < PRAYER_COUNT; index++)
				prayerWidgets.get(Prayer.values()[index].ordinal()).setHidden(false);
		}
	}

	private void hidePrayers()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		PrayerTabStates prayerTabState = getPrayerTabState();

		if (prayerTabState == PrayerTabStates.PRAYERS)
		{
			List<Widget> prayerWidgets = PRAYER_WIDGET_INFO_LIST.stream().map(client::getWidget)
					.filter(Objects::nonNull).collect(Collectors.toList());

			if (prayerWidgets.size() != PRAYER_WIDGET_INFO_LIST.size())
				return;

			for (int index = 0; index < PRAYER_COUNT; index++)
			{
				Prayer prayer = Prayer.values()[index];
				Widget prayerWidget = prayerWidgets.get(prayer.ordinal());

				if (!config.showPVPPrayers()
				&& !config.HideRapidRestore()
				&& !config.HideRapidHeal()
				&& !config.HideProtectItem()
				&& !config.HideSteelSkin()
				&& !config.HideUltimateStrength()
				&& !config.HideIncredibleReflex()
				&& !config.HidePTFMagic()
				&& !config.HidePTFRange()
				&& !config.HidePTFMelee()
				&& !config.HideEagle()
				&& !config.HideMystic()
				&& !config.HideRedemption()
				&& !config.HideSmite()
				&& !config.HidePreserve()
				&& !config.HideChivalry()
				&& !config.HidePiety()
				&& !config.HideRigour()
				&& !config.HideAugury())
				prayerWidget.setHidden(false);

				if (config.showPVPPrayers())
				{
					prayerWidget.setHidden(true);
					prayerWidgets.get(Prayer.values()[8].ordinal()).setHidden(false);// Rapid Restore
					prayerWidgets.get(Prayer.values()[9].ordinal()).setHidden(false);// Rapid Heal
					prayerWidgets.get(Prayer.values()[10].ordinal()).setHidden(false);// Protect Item
					prayerWidgets.get(Prayer.values()[13].ordinal()).setHidden(false);// Steel Skin
					prayerWidgets.get(Prayer.values()[14].ordinal()).setHidden(false);// Ultimate Strength
					prayerWidgets.get(Prayer.values()[15].ordinal()).setHidden(false);// Incredible Reflexes
					prayerWidgets.get(Prayer.values()[16].ordinal()).setHidden(false);// Protect from Magic
					prayerWidgets.get(Prayer.values()[17].ordinal()).setHidden(false);// Protect from Range
					prayerWidgets.get(Prayer.values()[18].ordinal()).setHidden(false);// Protect from Melee
					prayerWidgets.get(Prayer.values()[19].ordinal()).setHidden(false);// Eagle Eye
					prayerWidgets.get(Prayer.values()[20].ordinal()).setHidden(false);// Mystic Might
					prayerWidgets.get(Prayer.values()[22].ordinal()).setHidden(false);// Redemption
					prayerWidgets.get(Prayer.values()[23].ordinal()).setHidden(false);// Smite
					prayerWidgets.get(Prayer.values()[24].ordinal()).setHidden(false);// Preserve
					prayerWidgets.get(Prayer.values()[25].ordinal()).setHidden(false);// Chivalry
					prayerWidgets.get(Prayer.values()[26].ordinal()).setHidden(false);// Piety
					prayerWidgets.get(Prayer.values()[27].ordinal()).setHidden(false);// Rigour
					prayerWidgets.get(Prayer.values()[28].ordinal()).setHidden(false);// Augury

					if (config.HideRapidRestore())
					{
						prayerWidgets.get(Prayer.values()[8].ordinal()).setHidden(true);// Rapid Restore
					}

					if (config.HideRapidHeal())
					{
						prayerWidgets.get(Prayer.values()[9].ordinal()).setHidden(true);// Rapid Heal
					}

					if (config.HideProtectItem())
					{
						prayerWidgets.get(Prayer.values()[10].ordinal()).setHidden(true);// Protect Item
					}

					if (config.HideSteelSkin())
					{
						prayerWidgets.get(Prayer.values()[13].ordinal()).setHidden(true);// Steel Skin
					}

					if (config.HideUltimateStrength())
					{
						prayerWidgets.get(Prayer.values()[14].ordinal()).setHidden(true);// Ultimate Strength
					}

					if (config.HideIncredibleReflex())
					{
						prayerWidgets.get(Prayer.values()[15].ordinal()).setHidden(true);// Incredible Reflexes
					}

					if (config.HidePTFMagic())
					{
						prayerWidgets.get(Prayer.values()[16].ordinal()).setHidden(true);// Protect from Magic
					}

					if (config.HidePTFRange())
					{
						prayerWidgets.get(Prayer.values()[17].ordinal()).setHidden(true);// Protect from Range
					}

					if (config.HidePTFMelee())
					{
						prayerWidgets.get(Prayer.values()[18].ordinal()).setHidden(true);// Protect from Melee
					}

					if (config.HideEagle())
					{
						prayerWidgets.get(Prayer.values()[19].ordinal()).setHidden(true);// eagle eye
					}

					if (config.HideMystic())
					{
						prayerWidgets.get(Prayer.values()[20].ordinal()).setHidden(true);// Mystic Might
					}

					if (config.HideRedemption())
					{
						prayerWidgets.get(Prayer.values()[22].ordinal()).setHidden(true);// Redemption
					}

					if (config.HideSmite())
					{
						prayerWidgets.get(Prayer.values()[23].ordinal()).setHidden(true);// Smite
					}

					if (config.HidePreserve())
					{
						prayerWidgets.get(Prayer.values()[24].ordinal()).setHidden(true);// Preserve
					}

					if (config.HideChivalry())
					{
						prayerWidgets.get(Prayer.values()[25].ordinal()).setHidden(true);// Chivalry
					}

					if (config.HidePiety())
					{
						prayerWidgets.get(Prayer.values()[26].ordinal()).setHidden(true);// Piety
					}

					if (config.HideRigour())
					{
						prayerWidgets.get(Prayer.values()[27].ordinal()).setHidden(true);// Rigour
					}

					if (config.HideAugury())
					{
						prayerWidgets.get(Prayer.values()[28].ordinal()).setHidden(true);// Augury
					}
				}
			}
		}
	}
}